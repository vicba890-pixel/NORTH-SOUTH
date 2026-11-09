package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "region_states")
data class RegionEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val controller: String, // "player", "alliance_north", "rival_south", "neutral"
    val population: Long,
    val defenseLevel: Int,
    val latitude: Double,
    val longitude: Double,
    val isSpaceLanded: Boolean = false,
    val alloyOutput: Int = 10,
    val creditsOutput: Int = 20
)

@Entity(tableName = "tech_states")
data class TechEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String, // "Civilization", "Military", "Aerospace", "Signal"
    val cost: Int,
    val isUnlocked: Boolean,
    val progress: Int = 0,
    val equation: String = "" // Pure math definition shown in the tech tree
)

@Entity(tableName = "simulation_logs")
data class LogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val sender: String,
    val message: String,
    val type: String // "TACTICAL", "SYSTEM", "CHAT", "LANDING"
)

@Entity(tableName = "campaign_saves")
data class CampaignSaveEntity(
    @PrimaryKey val slotId: Int, // 1, 2, 3
    val timestamp: Long,
    val credits: Int,
    val alloys: Int,
    val globalPopulation: Long,
    val commanderName: String,
    val commanderFaction: String,
    val isLobbyJoined: Boolean,
    val serverRoomId: String,
    val selectedLanderStyle: String,
    val currentSpaceShipWeight: Int,
    val regionsJson: String,
    val techsJson: String,
    val gold: Double = 100.0,
    val food: Double = 50.0,
    val manpower: Double = 200.0,
    val goldCoeff: Double = 1.0,
    val foodCoeff: Double = 1.0,
    val manpowerCoeff: Double = 1.0
)

@Entity(tableName = "zkp_verified_moves")
data class ZkpMoveEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val faction: String, // "alliance_north", "rival_south", "player"
    val regionId: Int,
    val regionName: String,
    val actionType: String, // "ANNEX", "TAMPER_ALERT", "UPGRADE", "LANDING"
    val proofHash: String,
    val isVerified: Boolean = true,
    val details: String = "" // cryptographic params
)

@Dao
interface ZkpMoveDao {
    @Query("SELECT * FROM zkp_verified_moves ORDER BY timestamp DESC")
    fun getAllZkpMovesFlow(): Flow<List<ZkpMoveEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertZkpMove(move: ZkpMoveEntity)

    @Query("DELETE FROM zkp_verified_moves")
    suspend fun clearZkpMoves()
}

@Dao
interface RegionDao {
    @Query("SELECT * FROM region_states")
    fun getAllRegionsFlow(): Flow<List<RegionEntity>>

    @Query("SELECT * FROM region_states")
    suspend fun getAllRegions(): List<RegionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegions(regions: List<RegionEntity>)

    @Update
    suspend fun updateRegion(region: RegionEntity)

    @Query("DELETE FROM region_states")
    suspend fun deleteAllRegions()
}

@Dao
interface TechDao {
    @Query("SELECT * FROM tech_states")
    fun getAllTechsFlow(): Flow<List<TechEntity>>

    @Query("SELECT * FROM tech_states")
    suspend fun getAllTechs(): List<TechEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTechs(techs: List<TechEntity>)

    @Update
    suspend fun updateTech(tech: TechEntity)
}

@Dao
interface LogDao {
    @Query("SELECT * FROM simulation_logs ORDER BY timestamp DESC LIMIT 40")
    fun getRecentLogsFlow(): Flow<List<LogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: LogEntity)

    @Query("DELETE FROM simulation_logs")
    suspend fun clearLogs()
}

@Dao
interface CampaignSaveDao {
    @Query("SELECT * FROM campaign_saves WHERE slotId = :slotId")
    suspend fun getCampaignSave(slotId: Int): CampaignSaveEntity?

    @Query("SELECT * FROM campaign_saves")
    fun getAllCampaignSavesFlow(): Flow<List<CampaignSaveEntity>>

    @Query("SELECT * FROM campaign_saves")
    suspend fun getAllCampaignSaves(): List<CampaignSaveEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCampaignSave(save: CampaignSaveEntity)

    @Query("DELETE FROM campaign_saves WHERE slotId = :slotId")
    suspend fun deleteCampaignSave(slotId: Int)
}

@Database(
    entities = [RegionEntity::class, TechEntity::class, LogEntity::class, CampaignSaveEntity::class, ZkpMoveEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun regionDao(): RegionDao
    abstract fun techDao(): TechDao
    abstract fun logDao(): LogDao
    abstract fun campaignSaveDao(): CampaignSaveDao
    abstract fun zkpMoveDao(): ZkpMoveDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "north_south_world_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
