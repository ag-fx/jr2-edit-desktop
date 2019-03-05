package ru.jr2.edit.data.editc.repository

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.jr2.edit.EditApp
import ru.jr2.edit.data.editc.mapping.Edict
import java.io.File
import javax.xml.stream.XMLInputFactory

class EdictParserRepository(
    val xmlMapper: XmlMapper = EditApp.instance.xmlMapper
) {
    suspend inline fun <reified TEditc : Edict<TEntry>, TEntry> getEdictEntries(
        edictFile: File
    ): List<TEntry> = withContext(Dispatchers.IO) {
        val xmlStreamReader = XMLInputFactory
            .newInstance()
            .createXMLStreamReader(edictFile.inputStream())
        xmlMapper.readValue(xmlStreamReader, TEditc::class.java).entries
    }
}