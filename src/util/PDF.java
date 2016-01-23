package util;

import model.PharmacyStoreOrder;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;

public class PDF {
    public static void create(PharmacyStoreOrder order, String imagesDir, String saveDir) throws IOException {
        PDDocument document = new PDDocument();
        System.out.println(Constants.Database.DB_NAME);
        order.getOrderImages().forEach(img -> {
            try {
                //FileInputStream in = new FileInputStream(Constants.DOWNLOADING_DIR + Constants.FILE_SEPARATOR + imagesDir + Constants.FILE_SEPARATOR + img);
                FileInputStream in = new FileInputStream(imagesDir + Constants.Directory.FILE_SEPARATOR + img);
                BufferedImage bufferedImg = ImageIO.read(in);
                PDPage page = new PDPage(new PDRectangle(bufferedImg.getWidth(), bufferedImg.getHeight()));
                document.addPage(page);
                PDImageXObject pdfImg = PDImageXObject.createFromFile(imagesDir + Constants.Directory.FILE_SEPARATOR + img, document);
                PDPageContentStream contentStream = new PDPageContentStream(document, page);
                contentStream.drawImage(pdfImg, 0, 0);
                contentStream.close();
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        //document.save(Constants.CREATED_PDF_DIR + Constants.FILE_SEPARATOR + saveDir + Constants.FILE_SEPARATOR + String.format("patient%s_order%s.pdf", order.getPatientId(), order.getOrderId()));
        document.save(saveDir + Constants.Directory.FILE_SEPARATOR + String.format("patient%s_order%s.pdf", order.getPatientId(), order.getOrderId()));
        document.close();
    }
}
