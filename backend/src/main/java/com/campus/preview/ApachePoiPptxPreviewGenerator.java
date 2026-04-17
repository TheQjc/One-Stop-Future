package com.campus.preview;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.springframework.stereotype.Component;

@Component
public class ApachePoiPptxPreviewGenerator implements PptxPreviewGenerator {

    @Override
    public byte[] generate(InputStream pptxInputStream) throws IOException {
        Objects.requireNonNull(pptxInputStream, "pptxInputStream");

        try (XMLSlideShow slideShow = new XMLSlideShow(pptxInputStream);
                PDDocument document = new PDDocument();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Dimension pageSize = slideShow.getPageSize();
            float width = (float) pageSize.getWidth();
            float height = (float) pageSize.getHeight();

            for (var slide : slideShow.getSlides()) {
                BufferedImage image = renderSlide(slide, pageSize);
                PDPage page = new PDPage(new PDRectangle(width, height));
                document.addPage(page);
                PDImageXObject pdfImage = LosslessFactory.createFromImage(document, image);
                try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                    contentStream.drawImage(pdfImage, 0, 0, width, height);
                }
            }

            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }

    private BufferedImage renderSlide(org.apache.poi.xslf.usermodel.XSLFSlide slide, Dimension pageSize) {
        BufferedImage image = new BufferedImage(pageSize.width, pageSize.height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, pageSize.width, pageSize.height);
            slide.draw(graphics);
        } finally {
            graphics.dispose();
        }
        return image;
    }
}
