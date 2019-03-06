package ru.jr2.edit.domain.usecase

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import ru.jr2.edit.EditApp
import ru.jr2.edit.data.db.repository.KanjiDbRepository
import ru.jr2.edit.data.db.repository.KanjiReadingDbRepository
import ru.jr2.edit.data.db.table.KanjiComponentTable
import ru.jr2.edit.data.db.table.KanjiReadingTable
import ru.jr2.edit.data.db.table.KanjiTable
import ru.jr2.edit.domain.dto.KanjiDto
import ru.jr2.edit.domain.entity.KanjiEntity
import ru.jr2.edit.domain.misc.JlptLevel
import ru.jr2.edit.domain.model.KanjiModel
import ru.jr2.edit.domain.model.KanjiReadingModel

class KanjiDbUseCase(
    private val db: Database = EditApp.instance.db,
    private val kanjiDbRepository: KanjiDbRepository = KanjiDbRepository(db),
    private val kanjiReadingDbRepository: KanjiReadingDbRepository = KanjiReadingDbRepository(db)
) {
    fun getAllKanjiWithReadings(): List<KanjiDto> = transaction(db) {
        val kanjiMap = mutableMapOf<Int, KanjiDto>()
        KanjiTable.leftJoin(
            KanjiReadingTable,
            { KanjiTable.id },
            { KanjiReadingTable.kanji }
        ).selectAll().map {
            val kanjiId = it[KanjiTable.id].value
            if (!kanjiMap.containsKey(kanjiId)) {
                kanjiMap[kanjiId] = KanjiDto(
                    id = kanjiId,
                    kanji = it[KanjiTable.kanji],
                    interpretation = it[KanjiTable.interpretation] ?: "",
                    jlptLevel = JlptLevel.fromCode(it[KanjiTable.jlptLevel] ?: 0).str
                )
            }
            /* Проверка на null необходима не смотря на предупрждения компилятора
            , поскольку у канджи могут отсутствовать он-/кун- чтения */
            if (it[KanjiReadingTable.id] is EntityID<Int>) {
                val reading = it[KanjiReadingTable.reading]
                when (it[KanjiReadingTable.readingType]) {
                    0 -> kanjiMap[kanjiId]?.onReadings?.apply {
                        if (isNotEmpty()) append(", $reading") else append(reading)
                    }
                    1 -> kanjiMap[kanjiId]?.kunReadings?.apply {
                        if (isNotEmpty()) append(", $reading") else append(reading)
                    }
                }
            }
        }
        kanjiMap.values.toList()
    }

    fun getKanjiComponents(kanjiId: Int): List<KanjiModel> = transaction(db) {
        val componentAlias = KanjiComponentTable.alias("kanji_component")
        KanjiTable
            .innerJoin(
                componentAlias,
                { KanjiTable.id },
                { componentAlias[KanjiComponentTable.kanjiComponentId] }
            )
            .slice(KanjiTable.columns)
            .select {
                componentAlias[KanjiComponentTable.kanji] eq KanjiEntity[kanjiId].id
            }
            .orderBy(componentAlias[KanjiComponentTable.order])
            .map {
                KanjiModel.fromEntity(KanjiEntity.wrapRow(it))
            }
    }

    fun saveKanjiWithComponentsAndReadings(
        kanji: KanjiModel,
        readings: List<KanjiReadingModel>? = null,
        components: List<KanjiModel>? = null
    ): KanjiModel = transaction(db) {
        val newKanji = kanjiDbRepository.insertUpdate(kanji)
        readings?.run {
            this.forEach { it.kanji = newKanji.id }
            kanjiReadingDbRepository.insertUpdate(this)
        }
        components?.run { insertUpdateKanjiComponents(newKanji, this) }
        newKanji
    }

    fun deleteKanjiWithComponentsAndReadings(kanji: KanjiModel) = transaction(db) {
        /* Не смотря на то, что в foreign key стоит каскадное удаление,
        * exposed не хочет его производить, поэтому приходится иметь по
        * роуту для удаления каждого состоявляющего канджи */
        kanjiReadingDbRepository.deleteByKanjiId(kanji.id)
        deleteKanjiComponents(kanji)
        kanjiDbRepository.delete(kanji)
    }

    fun deleteKanjiWithComponentsAndReadings(kanjiId: Int) = transaction(db) {
        val kanjiToDelete = kanjiDbRepository.getById(kanjiId)
        deleteKanjiWithComponentsAndReadings(kanjiToDelete)
    }

    fun saveParsedKanjiWithReadings(
        kanjisWithReadings: List<Pair<KanjiModel, List<KanjiReadingModel>?>>
    ) = transaction(db) {
        val insertedKanjiId = KanjiTable.batchInsert(kanjisWithReadings.map { it.first }) {
            this[KanjiTable.kanji] = it.kanji
            this[KanjiTable.strokeCount] = it.strokeCount
            this[KanjiTable.interpretation] = it.interpretation
            this[KanjiTable.frequency] = it.frequency
            this[KanjiTable.grade] = it.grade
            this[KanjiTable.jlptLevel] = JlptLevel.fromStr(it.jlptLevel).code
        }.map {
            KanjiEntity.wrapRow(it).id.value
        }

        insertedKanjiId.forEachIndexed { idx, kanjiId ->
            kanjisWithReadings[idx].second?.forEach {
                it.kanji = kanjiId
            }
        }

        KanjiReadingTable.batchInsert(
            kanjisWithReadings
                .filter { it.second is List<KanjiReadingModel> }
                .flatMap { it.second!! }
        ) {
            this[KanjiReadingTable.reading] = it.reading
            this[KanjiReadingTable.readingType] = it.readingType
            this[KanjiReadingTable.priority] = it.priority
            this[KanjiReadingTable.isAnachronism] = it.isAnachronism
            this[KanjiReadingTable.kanji] = EntityID(it.kanji, KanjiTable)
        }
    }

    private fun insertUpdateKanjiComponents(kanji: KanjiModel, components: List<KanjiModel>) = transaction(db) {
        deleteKanjiComponents(kanji)
        var orderIdx = -1
        KanjiComponentTable.batchInsert(components) {
            this[KanjiComponentTable.kanji] = KanjiEntity[kanji.id].id
            this[KanjiComponentTable.kanjiComponentId] = KanjiEntity[it.id].id
            this[KanjiComponentTable.order] = ++orderIdx
        }
    }

    private fun deleteKanjiComponents(kanji: KanjiModel) = transaction(db) {
        KanjiComponentTable.deleteWhere {
            KanjiComponentTable.kanji eq kanji.id
        }
    }
}