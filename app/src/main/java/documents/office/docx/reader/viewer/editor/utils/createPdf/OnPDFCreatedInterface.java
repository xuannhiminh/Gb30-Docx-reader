package documents.office.docx.reader.viewer.editor.utils.createPdf;

public interface OnPDFCreatedInterface {
    void onPDFCreationStarted();
    void onPDFCreated(boolean success, String path);
}
