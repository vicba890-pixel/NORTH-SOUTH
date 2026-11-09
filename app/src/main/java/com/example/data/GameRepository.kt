package com.example.data

import kotlinx.coroutines.flow.Flow

class GameRepository(private val db: AppDatabase) {
    val regionDao = db.regionDao()
    val techDao = db.techDao()
    val logDao = db.logDao()
    val campaignSaveDao = db.campaignSaveDao()
    val zkpMoveDao = db.zkpMoveDao()

    val regions: Flow<List<RegionEntity>> = regionDao.getAllRegionsFlow()
    val techs: Flow<List<TechEntity>> = techDao.getAllTechsFlow()
    val logs: Flow<List<LogEntity>> = logDao.getRecentLogsFlow()
    val campaignSaves: Flow<List<CampaignSaveEntity>> = campaignSaveDao.getAllCampaignSavesFlow()
    val zkpMoves: Flow<List<ZkpMoveEntity>> = zkpMoveDao.getAllZkpMovesFlow()

    suspend fun insertRegions(regions: List<RegionEntity>) {
        regionDao.insertRegions(regions)
    }

    suspend fun deleteAllRegions() {
        regionDao.deleteAllRegions()
    }

    suspend fun updateRegion(region: RegionEntity) {
        regionDao.updateRegion(region)
    }

    suspend fun getAllRegions(): List<RegionEntity> {
        return regionDao.getAllRegions()
    }

    suspend fun getAllTechs(): List<TechEntity> {
        return techDao.getAllTechs()
    }

    suspend fun insertTechs(techs: List<TechEntity>) {
        techDao.insertTechs(techs)
    }

    suspend fun updateTech(tech: TechEntity) {
        techDao.updateTech(tech)
    }

    suspend fun insertLog(sender: String, message: String, type: String) {
        logDao.insertLog(LogEntity(sender = sender, message = message, type = type))
    }

    suspend fun clearLogs() {
        logDao.clearLogs()
    }

    suspend fun getCampaignSave(slotId: Int): CampaignSaveEntity? {
        return campaignSaveDao.getCampaignSave(slotId)
    }

    suspend fun insertCampaignSave(save: CampaignSaveEntity) {
        campaignSaveDao.insertCampaignSave(save)
    }

    suspend fun deleteCampaignSave(slotId: Int) {
        campaignSaveDao.deleteCampaignSave(slotId)
    }

    suspend fun insertZkpMove(move: ZkpMoveEntity) {
        zkpMoveDao.insertZkpMove(move)
    }

    suspend fun clearZkpMoves() {
        zkpMoveDao.clearZkpMoves()
    }
}
