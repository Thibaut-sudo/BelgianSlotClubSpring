package org.example.belgianslotclubspring.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ForumAttachmentStorageTest {

    @Test
    void detectAcceptsJpegMagic() {
        byte[] jpeg = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x00};
        ForumAttachmentStorage.Detected detected = ForumAttachmentStorage.detect(jpeg, "photo.bin", "application/octet-stream");
        assertNotNull(detected);
        assertEquals("jpg", detected.extension());
    }

    @Test
    void detectAcceptsPdfMagic() {
        byte[] pdf = "%PDF-1.4 extra".getBytes();
        ForumAttachmentStorage.Detected detected = ForumAttachmentStorage.detect(pdf, "doc.txt", "text/plain");
        assertNotNull(detected);
        assertEquals("pdf", detected.extension());
    }

    @Test
    void detectRejectsPdfWithoutMagic() {
        assertNull(ForumAttachmentStorage.detect("not a pdf".getBytes(), "malware.pdf", "application/pdf"));
    }

    @Test
    void detectRejectsUnknownBytes() {
        assertNull(ForumAttachmentStorage.detect(new byte[]{1, 2, 3, 4, 5, 6}, "file.exe", "application/x-msdownload"));
    }

    @Test
    void sanitizeKeepsReadableName() {
        assertEquals("photo.jpg", ForumAttachmentStorage.sanitizeOriginalName("/tmp/photo.jpg", "jpg"));
    }
}
