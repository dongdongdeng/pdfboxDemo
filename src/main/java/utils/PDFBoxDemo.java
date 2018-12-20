package utils;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.util.Matrix;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * PDFBox: Hello world!
 *
 */
public class PDFBoxDemo
{
    private static PDFont FONT = PDType1Font.HELVETICA;
    private static  float FONT_SIZE = 10;
    private static final float LEADING = -1.5f * FONT_SIZE;

    public static void showTextByLeft(PDPageContentStream overContent, String txt, String def, float x, float y) throws Exception{
        //Begin the Content stream
        overContent.beginText();

        if (null == txt) {
            txt = def;
        }

        //Setting the position for the line
        overContent.newLineAtOffset(x, y);

        //Adding text in the form of string
        overContent.showText(txt);

        //Ending the content stream
        overContent.endText();
    }

    /**
     * 增加一段文本
     * @param contentStream
     * @param width
     * @param sx
     * @param sy
     * @param text
     * @param justify
     * @throws IOException
     */
    private static void addParagraph(PDPageContentStream contentStream, float width, float sx,
                                     float sy, String text, boolean justify) throws IOException {
        List<String> lines = new ArrayList<>();
        parseLinesRecursive(text, width, lines);

        contentStream.setFont(FONT, FONT_SIZE);
        contentStream.newLineAtOffset(sx, sy);
        for (String line: lines) {
            float charSpacing = 0;
            if (justify){
                if (line.length() > 1) {
                    float size = FONT_SIZE * FONT.getStringWidth(line) / 1000;
                    float free = width - size;
                    if (free > 0 && !lines.get(lines.size() - 1).equals(line)) {
                        charSpacing = free / (line.length() - 1);
                    }
                }
            }
            contentStream.setCharacterSpacing(charSpacing);
            contentStream.showText(line);
            contentStream.newLineAtOffset(0, LEADING);
        }
    }

    /**
     * 递归分析文本，并按宽度分割成N行
     * @param text
     * @param width
     * @param lines
     * @return
     * @throws IOException
     */
    private static List<String> parseLinesRecursive(String text, float width, List<String> lines) throws IOException {
        String tmpText = text;
        for (int i=0; i<text.length(); i++) {
            tmpText = text.substring(0, text.length() - i);

            float realWidth = FONT_SIZE * FONT.getStringWidth(tmpText) / 1000;

            if (realWidth > width) {
                continue;
            } else {
                lines.add(tmpText);

                if (0 != i) {
                    parseLinesRecursive(text.substring(text.length() - i), width, lines);
                }

                break;
            }
        }

        return lines;
    }

    public static void main( String[] args ) throws Exception {
        try {
            // Loading an existing document
            InputStream in = PDFBoxDemo.class.getClassLoader().getResourceAsStream("VAT_INVOICE.pdf");
            PDDocument document = PDDocument.load(in);

            // Retrieving the pages of the document, and using PREPEND mode
            PDPage page = document.getPage(0);
            PDPageContentStream contentStream = new PDPageContentStream(document, page,
                    PDPageContentStream.AppendMode.PREPEND, false);

            InputStream inFont = PDFBoxDemo.class.getClassLoader().getResourceAsStream("simfang.ttf");
            PDType0Font font = PDType0Font.load(document, inFont);
            FONT = font;

            // Setting the font to the Content stream
            contentStream.setFont(font, 10);

            // 发票代码
            showTextByLeft(contentStream, "000000000211", "", 475,365);
            // 发票号码
            showTextByLeft(contentStream, "00000833", "", 475,350);
            // 开票日期
            showTextByLeft(contentStream, "2018年12月19日", "", 475,335);
            // 校验码
            showTextByLeft(contentStream, "12345 34567 56789 78901", "", 475,318);

            // 机器编号
            showTextByLeft(contentStream, "499098894300", "", 65,318);

            // 购买方 名称
            showTextByLeft(contentStream, "新龙门连锁客栈（上海）有限公司", "", 108,303);
            // 购买方 纳税人识别号
            showTextByLeft(contentStream, "95270000710929885W", "", 108,290);
            // 购买方 地址、电话
            showTextByLeft(contentStream, "上海万宝路大前门1234号 021-000000001", "", 108,275);

            // 备注 显示成多行
            String text = "增值税专用发票是由国家税务总局监制设计印制的，ABCDEFGHIJKLMNOPQRSTUVWXYZ只限于增值税一般纳税人领购使用的，作为纳税人反映经济活动中的重要会计凭证。" ;
            contentStream.beginText();
            addParagraph(contentStream, 200, 380, 95, text, true);
            contentStream.endText();

            // 开票人
            showTextByLeft(contentStream, "老张", "", 355,33);

            System.out.println("Content added");

            // Adding watermark
            for(PDPage everyPage:document.getPages()){
                PDPageContentStream cs = new PDPageContentStream(document, everyPage, PDPageContentStream.AppendMode.APPEND,
                        true, true);
                String ts = "测试";
                float fontSize = 80.0f;
                PDExtendedGraphicsState r0 = new PDExtendedGraphicsState();
                // 透明度
                r0.setNonStrokingAlphaConstant(0.3f);
                r0.setAlphaSourceFlag(true);

                cs.setGraphicsStateParameters(r0);
                cs.setNonStrokingColor(188,188,188);
                cs.beginText();
                cs.setFont(font, fontSize);
                // 获取旋转实例
                cs.setTextMatrix(Matrix.getRotateInstance(45,320f,150f));
                cs.showText(ts);
                cs.endText();

                cs.close();
            }

            // Loading img from file
            PDImageXObject pdImage = PDImageXObject.createFromFile("src/main/resources/song.png", document);

            // Draw image by  x y width height
            contentStream.drawImage(pdImage, 480, 10, 100, 100);

            // Closing the content stream
            contentStream.close();

            // Saving the document
            document.save(new File("..\\new_VAT_INVOICE.pdf"));

            // Closing the document
            document.close();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
