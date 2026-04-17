package com.campus.preview;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Rectangle;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import org.junit.jupiter.api.Test;

class ApachePoiPptxPreviewGeneratorTests {

    @Test
    void generatorTurnsSimplePptxIntoPdfBytes() throws IOException {
        ApachePoiPptxPreviewGenerator generator = new ApachePoiPptxPreviewGenerator();

        byte[] pdf = generator.generate(new ByteArrayInputStream(simplePptxBytes("Slide Title")));

        assertThat(new String(pdf, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
    }

    private byte[] simplePptxBytes(String title) throws IOException {
        try (XMLSlideShow slideShow = new XMLSlideShow();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            XSLFSlide slide = slideShow.createSlide();
            XSLFTextBox textBox = slide.createTextBox();
            textBox.setAnchor(new Rectangle(48, 48, 600, 100));
            XSLFTextParagraph paragraph = textBox.addNewTextParagraph();
            XSLFTextRun textRun = paragraph.addNewTextRun();
            textRun.setText(title);
            slideShow.write(outputStream);
            return outputStream.toByteArray();
        }
    }
}
