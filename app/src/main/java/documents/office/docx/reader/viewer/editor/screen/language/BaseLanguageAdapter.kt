package documents.office.docx.reader.viewer.editor.screen.language;

interface BaseLanguageAdapter {
    fun filter(query: String)
    fun getDisplayList(): List<ItemSelected>
    val selected: String
}