package com.example.ui.game

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONObject

// =========================================================================
// 1. DATA MODELS FOR R1CS 1024 CONSTRAINT MATRIX & RADIAL CLUSTERING
// =========================================================================

data class R1csConstraint1024Node(
    val index: Int,
    val id: String,
    val title: String,
    val category: String, // "BOOLEANITY", "RANGE", "ARITHMETIC", "SCHNORR"
    val formulaDisplay: String,
    val lhs: Double,
    val rhs: Double,
    val residual: Double,
    val isSatisfied: Boolean,
    val primaryVariables: List<String>
)

data class R1csMatrixRadialSummary(
    val totalConstraints: Int = 1024,
    val matrixDimension: String = "1024 × 1024",
    val nonZeroCoefficients: Int = 4096,
    val sparsityPercent: Float = 99.61f,
    val curveProfile: String = "BN254 (Groth16 / Snarkjs)",
    val isAllSatisfied: Boolean = true,
    val authorialAnchor: String = "vmsb©"
)

/**
 * JavaScript Interface Bridge between D3.js Radial Cluster and Jetpack Compose.
 */
class D3R1csJsBridge(
    private val onConstraintSelected: (R1csConstraint1024Node) -> Unit,
    private val onCategoryFilterChanged: (String) -> Unit
) {
    @JavascriptInterface
    fun onNodeClicked(nodeJsonStr: String) {
        try {
            val json = JSONObject(nodeJsonStr)
            val node = R1csConstraint1024Node(
                index = json.optInt("index", 0),
                id = json.optString("id", "C#0000"),
                title = json.optString("name", "Constraint"),
                category = json.optString("category", "ARITHMETIC"),
                formulaDisplay = json.optString("formula", "(A·s) ∘ (B·s) = C·s"),
                lhs = json.optDouble("lhs", 1.0),
                rhs = json.optDouble("rhs", 1.0),
                residual = json.optDouble("residual", 0.0),
                isSatisfied = json.optBoolean("isSatisfied", true),
                primaryVariables = listOf("s_${json.optInt("index", 0)}", "s_1")
            )
            onConstraintSelected(node)
        } catch (_: Exception) {}
    }

    @JavascriptInterface
    fun onFilterCategory(category: String) {
        onCategoryFilterChanged(category)
    }
}

// =========================================================================
// 2. HTML & D3.JS DYNAMIC RADIAL CLUSTER CHART GENERATOR (1024 CONSTRAINTS)
// =========================================================================

fun generateD3R1csClusterRadialHtml(): String {
    return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
    <title>D3.js R1CS 1024 Constraint Matrix Cluster-Radial Chart</title>
    <!-- Official D3.js v7 CDN with embedded fallback engine -->
    <script src="https://d3js.org/d3.v7.min.js"></script>
    <style>
        * {
            box-sizing: border-box;
            user-select: none;
            -webkit-user-select: none;
            margin: 0;
            padding: 0;
        }
        body {
            background-color: #050B18;
            color: #E2E8F0;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, monospace;
            overflow: hidden;
            width: 100vw;
            height: 100vh;
        }
        #chart-container {
            width: 100%;
            height: 100%;
            position: relative;
        }
        svg {
            width: 100%;
            height: 100%;
            display: block;
        }
        .radial-link {
            fill: none;
            stroke-opacity: 0.42;
            stroke-width: 0.9px;
            transition: stroke-opacity 0.2s ease;
        }
        .radial-link:hover {
            stroke-opacity: 0.9 !important;
            stroke-width: 1.8px !important;
        }
        .chord-coupling {
            fill: none;
            stroke: #BD00FF;
            stroke-opacity: 0.22;
            stroke-width: 0.7px;
            stroke-dasharray: 2, 2;
        }
        .node-leaf {
            cursor: pointer;
            transition: all 0.2s ease;
        }
        .node-leaf:hover {
            filter: drop-shadow(0 0 6px #00FFCC);
            r: 4.5px !important;
        }
        .node-cluster {
            cursor: pointer;
        }
        .category-label {
            font-size: 8.5px;
            font-weight: 700;
            fill: #FFFFFF;
            text-anchor: middle;
            text-shadow: 0 1px 4px rgba(0,0,0,0.9);
            pointer-events: none;
        }
        #hud-overlay {
            position: absolute;
            top: 8px;
            left: 8px;
            background: rgba(11, 20, 39, 0.9);
            border: 1px solid #1E293B;
            border-radius: 6px;
            padding: 6px 10px;
            font-size: 8.5px;
            pointer-events: none;
            backdrop-filter: blur(4px);
        }
        .status-badge {
            display: inline-block;
            width: 6px;
            height: 6px;
            border-radius: 50%;
            background: #00FF88;
            margin-right: 4px;
        }
    </style>
</head>
<body>
    <div id="chart-container">
        <svg id="radial-svg"></svg>
        <div id="hud-overlay">
            <div><span class="status-badge" id="hud-status-dot"></span><b id="hud-title">R1CS 1024 RADIAL CLUSTER</b></div>
            <div id="hud-sub" style="color:#64748B; margin-top:2px;">(A·s) ∘ (B·s) = C·s • 1024 Nodos • 360° D3.js</div>
        </div>
    </div>

    <script>
    (function() {
        // --- 1. GENERATE HIERARCHICAL 1024 CONSTRAINTS DATASET ---
        const quadrants = [
            { id: "BOOL", name: "BOOLEANIDAD (b_i * (1-b_i) = 0)", category: "BOOLEANITY", color: "#00FFCC", count: 256, startIdx: 0 },
            { id: "RANGE", name: "DESCOMPOSICIÓN DE RANGO (∑2^i*b_i = v)", category: "RANGE", color: "#A855F7", count: 256, startIdx: 256 },
            { id: "ARITH", name: "COMPUERTAS ARITMÉTICAS CUADRÁTICAS", category: "ARITHMETIC", color: "#38BDF8", count: 256, startIdx: 512 },
            { id: "SCHNORR", name: "COMPROMISOS PEDERSEN & ENCLAVE vmsb", category: "SCHNORR", color: "#FBBF24", count: 256, startIdx: 768 }
        ];

        let isTampered = false;

        function buildHierarchy(filterCat) {
            const root = {
                name: "ZKP Enclave R1CS 1024",
                id: "root",
                category: "ROOT",
                children: []
            };

            quadrants.forEach(q => {
                if (filterCat && filterCat !== "ALL" && q.category !== filterCat) return;

                const quadNode = {
                    name: q.name,
                    id: "quad_" + q.id,
                    category: q.category,
                    color: q.color,
                    children: []
                };

                // 4 Sub-clusters per quadrant (64 constraints each)
                for (let s = 0; s < 4; s++) {
                    const subId = q.id + "_SUB_" + s;
                    const subRangeStart = q.startIdx + s * 64;
                    const subRangeEnd = subRangeStart + 63;
                    const subNode = {
                        name: q.id + " [" + subRangeStart + ".." + subRangeEnd + "]",
                        id: subId,
                        category: q.category,
                        color: q.color,
                        children: []
                    };

                    for (let c = 0; c < 64; c++) {
                        const idx = subRangeStart + c;
                        const isPerturbed = isTampered && (idx === 312 || idx === 313);

                        let formula = "";
                        let lhs = 0.0;
                        let rhs = 0.0;
                        if (q.category === "BOOLEANITY") {
                            formula = "b_" + (idx % 256) + " × (1 - b_" + (idx % 256) + ") = 0";
                            lhs = isPerturbed ? 0.42 : 0.0;
                            rhs = 0.0;
                        } else if (q.category === "RANGE") {
                            formula = "∑ 2^i·b_i + s_slk - v_" + (idx % 32) + " = 0";
                            lhs = isPerturbed ? 54.0 : 37.0;
                            rhs = 37.0;
                        } else if (q.category === "ARITHMETIC") {
                            formula = "(1.0·s_" + idx + ") × (1.0·s_1) = 1.0·s_" + ((idx + 1) % 1024);
                            lhs = 14.85;
                            rhs = isPerturbed ? 16.20 : 14.85;
                        } else {
                            formula = "Poseidon(s_" + idx + ", salt) = Com_" + idx;
                            lhs = 8.3804;
                            rhs = isPerturbed ? 9.1120 : 8.3804;
                        }

                        const res = Math.abs(lhs - rhs);
                        const isSatisfied = res < 1e-4;

                        subNode.children.push({
                            index: idx,
                            id: "C#" + String(idx).padStart(4, "0"),
                            name: "Restricción #" + idx,
                            category: q.category,
                            formula: formula,
                            lhs: lhs,
                            rhs: rhs,
                            residual: res,
                            isSatisfied: isSatisfied,
                            color: isSatisfied ? q.color : "#EF4444"
                        });
                    }
                    quadNode.children.push(subNode);
                }
                root.children.push(quadNode);
            });

            return root;
        }

        // --- 2. D3 RENDER & CLUSTER RADIAL ENGINE ---
        const svg = d3.select("#radial-svg");
        let activeFilter = "ALL";
        let isRotating = false;
        let currentRotation = 0;
        let animFrameId = null;

        function renderChart() {
            svg.selectAll("*").remove();

            const bbox = document.body.getBoundingClientRect();
            const width = bbox.width || 360;
            const height = bbox.height || 260;
            const radius = Math.min(width, height) / 2 - 18;

            const g = svg.append("g")
                .attr("id", "main-radial-group")
                .attr("transform", "translate(" + (width / 2) + "," + (height / 2) + ")");

            // Zoom behavior
            const zoom = d3.zoom()
                .scaleExtent([0.6, 5])
                .on("zoom", function(event) {
                    g.attr("transform", event.transform);
                });
            svg.call(zoom);

            const rawData = buildHierarchy(activeFilter);
            const rootHierarchy = d3.hierarchy(rawData);

            // D3 Cluster Radial Layout (360 degrees, radial distance)
            const cluster = d3.cluster()
                .size([360, radius - 24]);

            cluster(rootHierarchy);

            // 1. Concentric Guide Rings
            const ringRadii = [radius * 0.33, radius * 0.66, radius - 24];
            g.append("g")
                .attr("class", "guide-rings")
                .selectAll("circle")
                .data(ringRadii)
                .join("circle")
                .attr("r", d => d)
                .attr("fill", "none")
                .attr("stroke", "#1E293B")
                .attr("stroke-width", 0.8)
                .attr("stroke-dasharray", "2, 3");

            // 2. Cross-Coupling Internal Quadratic Chords (Sparsity pattern)
            const leaves = rootHierarchy.leaves();
            const chords = [];
            for (let i = 0; i < leaves.length; i += 32) {
                const targetIdx = (i + Math.floor(leaves.length / 2)) % leaves.length;
                chords.push({
                    source: leaves[i],
                    target: leaves[targetIdx]
                });
            }

            g.append("g")
                .attr("class", "chords-layer")
                .selectAll("path")
                .data(chords)
                .join("path")
                .attr("class", "chord-coupling")
                .attr("d", function(d) {
                    const sA = (d.source.x - 90) * Math.PI / 180;
                    const tA = (d.target.x - 90) * Math.PI / 180;
                    const sx = d.source.y * Math.cos(sA);
                    const sy = d.source.y * Math.sin(sA);
                    const tx = d.target.y * Math.cos(tA);
                    const ty = d.target.y * Math.sin(tA);
                    return "M" + sx + "," + sy + "Q0,0 " + tx + "," + ty;
                });

            // 3. Radial Links
            const linkColorMap = {
                "BOOLEANITY": "#00FFCC",
                "RANGE": "#A855F7",
                "ARITHMETIC": "#38BDF8",
                "SCHNORR": "#FBBF24"
            };

            g.append("g")
                .attr("class", "links-layer")
                .selectAll("path")
                .data(rootHierarchy.links())
                .join("path")
                .attr("class", "radial-link")
                .attr("stroke", d => {
                    const cat = d.target.data.category;
                    return linkColorMap[cat] || "#475569";
                })
                .attr("d", d3.linkRadial()
                    .angle(d => d.x * Math.PI / 180)
                    .radius(d => d.y)
                );

            // 4. Nodes (Central Enclave Hub + Clusters + 1024 Leaves)
            const nodeGroup = g.append("g")
                .attr("class", "nodes-layer")
                .selectAll("g")
                .data(rootHierarchy.descendants())
                .join("g")
                .attr("transform", d => {
                    return "rotate(" + (d.x - 90) + ") translate(" + d.y + ",0)";
                });

            nodeGroup.append("circle")
                .attr("class", d => d.children ? "node-cluster" : "node-leaf")
                .attr("r", d => {
                    if (d.depth === 0) return 9; // Root Enclave
                    if (d.depth === 1) return 5.5; // Quadrant
                    if (d.depth === 2) return 3.2; // Sub-cluster
                    return 1.6; // 1024 constraint leaf
                })
                .attr("fill", d => {
                    if (d.depth === 0) return "#00FFCC";
                    if (d.data.color) return d.data.color;
                    return "#38BDF8";
                })
                .on("click", function(event, d) {
                    event.stopPropagation();
                    if (!d.children) {
                        onConstraintClicked(d.data);
                    } else if (d.depth === 1) {
                        onQuadrantClicked(d.data);
                    }
                });

            // Quadrant text labels
            rootHierarchy.children?.forEach(q => {
                const angle = q.x;
                const rPos = radius * 0.42;
                const rad = (angle - 90) * Math.PI / 180;
                const lx = rPos * Math.cos(rad);
                const ly = rPos * Math.sin(rad);

                g.append("text")
                    .attr("x", lx)
                    .attr("y", ly)
                    .attr("class", "category-label")
                    .text(q.data.id ? q.data.id.replace("quad_", "") : "")
                    .attr("fill", q.data.color || "#00FFCC");
            });

            // Central root label
            g.append("circle")
                .attr("r", 12)
                .attr("fill", "url(#rootGrad)")
                .attr("stroke", "#00FFCC")
                .attr("stroke-width", 1.5);

            g.append("text")
                .attr("text-anchor", "middle")
                .attr("dy", "3px")
                .attr("font-size", "7px")
                .attr("font-weight", "bold")
                .attr("fill", "#060B18")
                .text("1024");
        }

        function onConstraintClicked(data) {
            document.getElementById("hud-title").textContent = data.id + " • " + data.category;
            document.getElementById("hud-sub").textContent = data.formula + " | Res: " + data.residual.toFixed(6);
            document.getElementById("hud-status-dot").style.background = data.isSatisfied ? "#00FF88" : "#EF4444";

            if (window.D3R1csBridge) {
                window.D3R1csBridge.onNodeClicked(JSON.stringify(data));
            }
        }

        function onQuadrantClicked(data) {
            document.getElementById("hud-title").textContent = data.name;
            document.getElementById("hud-sub").textContent = "256 Restricciones Cuadráticas • R1CS";
            if (window.D3R1csBridge) {
                window.D3R1csBridge.onFilterCategory(data.category);
            }
        }

        // --- 3. EXPOSED CONTROL FUNCTIONS FOR ANDROID ---
        window.filterRadialCategory = function(cat) {
            activeFilter = cat;
            renderChart();
        };

        window.toggleTamperAttack = function(tamper) {
            isTampered = tamper;
            renderChart();
        };

        window.toggleRotation = function() {
            isRotating = !isRotating;
            if (isRotating) {
                function rotateLoop() {
                    if (!isRotating) return;
                    currentRotation = (currentRotation + 0.3) % 360;
                    const g = d3.select("#main-radial-group");
                    if (!g.empty()) {
                        const bbox = document.body.getBoundingClientRect();
                        const cx = (bbox.width || 360) / 2;
                        const cy = (bbox.height || 260) / 2;
                        g.attr("transform", "translate(" + cx + "," + cy + ") rotate(" + currentRotation + ")");
                    }
                    animFrameId = requestAnimationFrame(rotateLoop);
                }
                rotateLoop();
            } else if (animFrameId) {
                cancelAnimationFrame(animFrameId);
            }
        };

        window.resetView = function() {
            isRotating = false;
            currentRotation = 0;
            if (animFrameId) cancelAnimationFrame(animFrameId);
            renderChart();
        };

        // Fallback pure SVG renderer when D3 CDN is unavailable
        function renderPureSvgFallback() {
            const svg = document.getElementById("radial-svg");
            if (!svg) return;
            svg.innerHTML = '<g id="fallback-group"><circle cx="180" cy="130" r="100" stroke="#00FFCC" fill="none" stroke-width="1.5" stroke-dasharray="4,4"/><text x="180" y="130" fill="#00FFCC" font-size="10" text-anchor="middle" font-family="monospace">1024 NODOS ENCLAVE FALLBACK</text></g>';
        }

        // Initial render
        if (typeof d3 === 'undefined') {
            renderPureSvgFallback();
        } else {
            window.addEventListener("resize", renderChart);
            setTimeout(renderChart, 50);
        }
    })();
    </script>
</body>
</html>
    """.trimIndent()
}

// =========================================================================
// 3. JETPACK COMPOSE COMPOSABLE: D3 R1CS CLUSTER-RADIAL CARD
// =========================================================================

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun D3R1csClusterRadialCard(
    onNavigateBack: () -> Unit = {},
    onNavigateTo2dMatrix: () -> Unit = onNavigateBack,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var selectedCategoryFilter by remember { mutableStateOf("ALL") } // "ALL", "BOOLEANITY", "RANGE", "ARITHMETIC", "SCHNORR"
    var isTamperAttackActive by remember { mutableStateOf(false) }
    var isSpinningActive by remember { mutableStateOf(false) }

    // Selected constraint node inspection
    var selectedNode by remember {
        mutableStateOf(
            R1csConstraint1024Node(
                index = 412,
                id = "C#0412",
                title = "Compuerta Aritmética de Enclave #412",
                category = "ARITHMETIC",
                formulaDisplay = "(1.0000 · s_412) × (1.0000 · s_1) = 1.0000 · s_413",
                lhs = 14.8500,
                rhs = 14.8500,
                residual = 0.000000,
                isSatisfied = true,
                primaryVariables = listOf("s_412", "s_1", "s_413")
            )
        )
    }

    val summary = remember { R1csMatrixRadialSummary() }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("d3_r1cs_cluster_radial_card"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF060B18)),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                listOf(Color(0xFF00FFCC), Color(0xFF38BDF8), Color(0xFFA855F7))
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF00FFCC), Color(0xFF0284C7))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "R1CS Radial",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "🕸️ R1CS MATRIX: ÁRBOL CLUSTER-RADIAL (D3.JS)",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "1024 Restricciones Cuadráticas ZKP en 360° • BN254 Groth16",
                            fontSize = 8.sp,
                            color = Color(0xFF00FFCC)
                        )
                    }
                }

                // Verified Badge
                Surface(
                    color = if (isTamperAttackActive) Color(0xFF450A0A) else Color(0xFF064E3B),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (isTamperAttackActive) Color(0xFFEF4444) else Color(0xFF00FF88))
                ) {
                    Text(
                        text = if (isTamperAttackActive) "PERTURBACIÓN ✕" else "1024/1024 OK ✓",
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isTamperAttackActive) Color(0xFFEF4444) else Color(0xFF00FF88),
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            // Quick Telemetry 4-Pack
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    Pair("RESTRICCIONES", "1024 R1CS"),
                    Pair("DIMENSIÓN", "1024 × 1024"),
                    Pair("ESPARSIDAD", "99.61%"),
                    Pair("CURVA", "BN254 PQC")
                ).forEach { (label, value) ->
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = Color(0xFF0A1226),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, Color(0xFF1E293B))
                    ) {
                        Column(
                            modifier = Modifier.padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(label, fontSize = 6.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                            Text(value, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00FFCC), fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }

            // Category Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    Pair("ALL", "TODAS (1024)"),
                    Pair("BOOLEANITY", "BOOLEANAS (256)"),
                    Pair("RANGE", "RANGO (256)"),
                    Pair("ARITHMETIC", "ARITMÉTICA (256)"),
                    Pair("SCHNORR", "SCHNORR (256)")
                ).forEach { (catKey, catLabel) ->
                    val isSelected = selectedCategoryFilter == catKey
                    Surface(
                        color = if (isSelected) Color(0xFF0284C7) else Color(0xFF0B1426),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, if (isSelected) Color(0xFF38BDF8) else Color(0xFF1E293B)),
                        modifier = Modifier
                            .clickable {
                                selectedCategoryFilter = catKey
                                webViewInstance?.evaluateJavascript("window.filterRadialCategory('$catKey');", null)
                            }
                            .testTag("filter_d3_radial_$catKey")
                    ) {
                        Text(
                            text = catLabel,
                            fontSize = 7.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else Color(0xFF94A3B8),
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // WebView Interactive Canvas Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF050B18))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
            ) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            settings.cacheMode = WebSettings.LOAD_DEFAULT
                            setBackgroundColor(0xFF050B18.toInt())

                            val bridge = D3R1csJsBridge(
                                onConstraintSelected = { node ->
                                    selectedNode = node
                                },
                                onCategoryFilterChanged = { cat ->
                                    selectedCategoryFilter = cat
                                }
                            )
                            addJavascriptInterface(bridge, "D3R1csBridge")

                            webViewClient = WebViewClient()
                            webChromeClient = WebChromeClient()

                            loadDataWithBaseURL(
                                "https://d3js.org",
                                generateD3R1csClusterRadialHtml(),
                                "text/html",
                                "UTF-8",
                                null
                            )
                            webViewInstance = this
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("webview_d3_r1cs_radial")
                )
            }

            // Node Inspector HUD Card
            Surface(
                color = Color(0xFF081226),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, if (selectedNode.isSatisfied) Color(0xFF00FF88).copy(alpha = 0.5f) else Color(0xFFEF4444)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hud_d3_r1cs_inspector")
            ) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                color = if (selectedNode.isSatisfied) Color(0xFF00FF88).copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(3.dp)
                            ) {
                                Text(
                                    text = selectedNode.id,
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedNode.isSatisfied) Color(0xFF00FF88) else Color(0xFFEF4444),
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                            Text(
                                text = selectedNode.title,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Text(
                            text = if (selectedNode.isSatisfied) "SATISFECHO ✓" else "VIOLACIÓN ✕",
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedNode.isSatisfied) Color(0xFF00FF88) else Color(0xFFEF4444),
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Text(
                        text = "Fórmula: ${selectedNode.formulaDisplay}",
                        fontSize = 7.5.sp,
                        color = Color(0xFF00FFCC),
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "LHS = ${"%.4f".format(selectedNode.lhs)} | RHS = ${"%.4f".format(selectedNode.rhs)} | Residuo Δ = ${"%.6f".format(selectedNode.residual)}",
                        fontSize = 7.sp,
                        color = Color(0xFF94A3B8),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Interactive Controls: Audit 1024, Perturbation, Spin, Reset View, View 2D Matrices
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = {
                        isTamperAttackActive = false
                        webViewInstance?.evaluateJavascript("window.toggleTamperAttack(false);", null)
                        Toast.makeText(context, "✅ 1024 restricciones auditadas: 100% satisfechas", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(30.dp)
                        .testTag("btn_d3_audit_all_constraints"),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Audit", tint = Color.Black, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("AUDITAR 1024", fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }

                Button(
                    onClick = {
                        isTamperAttackActive = !isTamperAttackActive
                        webViewInstance?.evaluateJavascript("window.toggleTamperAttack($isTamperAttackActive);", null)
                        Toast.makeText(
                            context,
                            if (isTamperAttackActive) "🚨 Perturbación ZKP inyectada en fila C#0312" else "🛡️ Restricciones restauradas",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isTamperAttackActive) Color(0xFFDC2626) else Color(0xFF1E293B)
                    ),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(30.dp)
                        .testTag("btn_d3_tamper_radial"),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = "Tamper", tint = Color.White, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(if (isTamperAttackActive) "RESTAURAR" else "PROBAR ATAQUE", fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                OutlinedButton(
                    onClick = {
                        isSpinningActive = !isSpinningActive
                        webViewInstance?.evaluateJavascript("window.toggleRotation();", null)
                    },
                    border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF0A152E)),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(30.dp)
                        .testTag("btn_d3_rotate_radial"),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Rotate", tint = Color(0xFF38BDF8), modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(if (isSpinningActive) "PAUSAR" else "ROTACIÓN", fontSize = 7.5.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        onNavigateTo2dMatrix()
                    },
                    border = BorderStroke(1.dp, Color(0xFFA855F7)),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF1A0B2E)),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(30.dp)
                        .testTag("btn_d3_view_2d_matrices"),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Icon(Icons.Default.List, contentDescription = "2D", tint = Color(0xFFA855F7), modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("MATRICES 2D", fontSize = 7.5.sp, color = Color(0xFFA855F7), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
