package ru.jr2.edit.domain.entity

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import ru.jr2.edit.data.db.table.KanjiReadingTable
import ru.jr2.edit.data.db.table.KanjiTable
import ru.jr2.edit.presentation.kanji.model.KanjiReadingModel
import ru.jr2.edit.util.KanjiReadingType

class KanjiReadingEntity(id: EntityID<Int>) : IntEntity(id) {
    var reading by KanjiReadingTable.reading
    var readingType by KanjiReadingTable.readingType
    var priority by KanjiReadingTable.priority
    var isAnachronism by KanjiReadingTable.isAnachronism
    var kanji by KanjiReadingTable.kanji

    fun updateWithModel(model: KanjiReadingModel) {
        reading = model.reading
        readingType = KanjiReadingType.fromStr(model.readingType).code
        priority = model.priority
        isAnachronism = model.isAnachronism
        kanji = EntityID(model.kanji, KanjiTable)
    }

    companion object : IntEntityClass<KanjiReadingEntity>(KanjiReadingTable)
}