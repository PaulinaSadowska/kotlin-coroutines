/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.advancedcoroutines

import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import com.example.android.advancedcoroutines.util.CacheOnSuccess
import com.example.android.advancedcoroutines.utils.ComparablePair
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext

/**
 * Repository module for handling data operations.
 *
 * This PlantRepository exposes two UI-observable database queries [plants] and
 * [getPlantsWithGrowZone].
 *
 * To update the plants cache, call [tryUpdateRecentPlantsForGrowZoneCache] or
 * [tryUpdateRecentPlantsCache].
 */
@FlowPreview
class PlantRepository private constructor(
        private val plantDao: PlantDao,
        private val plantService: NetworkService,
        private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) {

    /**
     * Fetch a list of [Plant]s from the database.
     * Returns a LiveData-wrapped List of Plants.
     */
    val plants: LiveData<List<Plant>> = plantDao.getPlants().switchMap {
        liveData {
            val sortOrder = plantListSortOrderCache.getOrAwait()
            emit(it.applyMainSafeSort(sortOrder))
        }
    }

    private val plantListSortOrderCache = CacheOnSuccess(onErrorFallback = { listOf() }) {
        plantService.customPlantSortOrder()
    }

    private fun List<Plant>.applySort(customSortOrder: List<String>): List<Plant> {
        return this.sortedBy { plant ->
            val position = customSortOrder.indexOf(plant.plantId).takeIf { it >= 0 }
            ComparablePair(position ?: Int.MAX_VALUE, plant.name)
        }
    }

    private suspend fun List<Plant>.applyMainSafeSort(customSortOrder: List<String>): List<Plant> {
        return withContext(defaultDispatcher) {
            applySort(customSortOrder)
        }
    }

    /**
     * Fetch a list of [Plant]s from the database that matches a given [GrowZone].
     * Returns a LiveData-wrapped List of Plants.
     */
    fun getPlantsWithGrowZone(growZone: GrowZone) = plantDao
            .getPlantsWithGrowZoneNumber(growZone.number)
            .switchMap { plants ->
                liveData {
                    val sortOrder = plantListSortOrderCache.getOrAwait()
                    emit(plants.applyMainSafeSort(sortOrder))
                }
            }



    // - more complicated
    // + runs concurrently
    // fetches sort order only when changed
    val plantsFlow: Flow<List<Plant>>
        get() = plantDao.getPlantsAsFlow().combine(customSortFlow) { plants, sortOrder ->
            plants.applySort(sortOrder)
        }.flowOn(defaultDispatcher)
                .conflate()

    private val customSortFlow = plantListSortOrderCache::getOrAwait.asFlow()

    // + more simple
    // - it runs in sequence
    // - fetches sort order every time growZone changes (it's ok because we have cache)
    fun getPlantsFlowWithGrowZone(growZone: GrowZone) =
            plantDao.getPlantsWithGrowZoneNumberAsFlow(growZone.number).map{ plants ->
                val sortOrder = plantListSortOrderCache.getOrAwait()
                val sorted = plants.applyMainSafeSort(sortOrder)
                sorted
            }

    /**
     * Returns true if we should make a network request.
     */
    private fun shouldUpdatePlantsCache(): Boolean {
        // suspending function, so you can e.g. check the status of the database here
        return true
    }

    /**
     * Update the plants cache.
     *
     * This function may decide to avoid making a network requests on every call based on a
     * cache-invalidation policy.
     */
    suspend fun tryUpdateRecentPlantsCache() {
        if (shouldUpdatePlantsCache()) fetchRecentPlants()
    }

    /**
     * Update the plants cache for a specific grow zone.
     *
     * This function may decide to avoid making a network requests on every call based on a
     * cache-invalidation policy.
     */
    suspend fun tryUpdateRecentPlantsForGrowZoneCache(growZoneNumber: GrowZone) {
        if (shouldUpdatePlantsCache()) fetchPlantsForGrowZone(growZoneNumber)
    }

    /**
     * Fetch a new list of plants from the network, and append them to [plantDao]
     */
    private suspend fun fetchRecentPlants() {
        val plants = plantService.allPlants()
        plantDao.insertAll(plants)
    }

    /**
     * Fetch a list of plants for a grow zone from the network, and append them to [plantDao]
     */
    private suspend fun fetchPlantsForGrowZone(growZone: GrowZone) {
        val plants = plantService.plantsByGrowZone(growZone)
        plantDao.insertAll(plants)
    }

    companion object {

        // For Singleton instantiation
        @Volatile
        private var instance: PlantRepository? = null

        fun getInstance(plantDao: PlantDao, plantService: NetworkService) =
                instance ?: synchronized(this) {
                    instance ?: PlantRepository(plantDao, plantService).also { instance = it }
                }
    }
}
