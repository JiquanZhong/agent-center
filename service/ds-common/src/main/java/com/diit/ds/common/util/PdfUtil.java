package com.diit.ds.common.util;

import com.aspose.cells.PdfSaveOptions;
import com.aspose.cells.Workbook;
import com.aspose.words.Document;
import com.aspose.words.FontSettings;
import com.aspose.words.License;
import com.aspose.words.SaveFormat;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;


/**
 * 转PDF工具类，支持word、txt、excel、ppt
 *
 * @author free
 */
@Slf4j
public class PdfUtil {

    //    public static void main(String[] args) {
//        try {
//            String filePath1 = "C:\\Users\\test\\Desktop\\测试文档\\测试文档1.docx";
//            String filePath2 = "C:\\Users\\test\\Desktop\\测试文档\\测试文档2.txt";
//            String filePath3 = "C:\\Users\\test\\Desktop\\测试文档\\测试文档3.xlsx";
//            String filePath4 = "C:\\Users\\test\\Desktop\\测试文档\\测试文档4.pptx";
//
//            String wordPath = wordToPdf(filePath1);
//            System.out.println(wordPath);
//            String txtPath = txtToPdf(filePath2);
//            System.out.println(txtPath);
//            String excelPath = excelToPdf(filePath3);
//            System.out.println(excelPath);
//            String pptPath = pptToPdf(filePath4);
//            System.out.println(pptPath);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    public static String toPdf(String inPath) {
        String outPath = inPath.substring(0, inPath.lastIndexOf(".")) + ".pdf";
        String ext = inPath.substring(inPath.lastIndexOf(".") + 1).toLowerCase();
        if (ext.equals("xls") || ext.equals("xlsx")) {
            excelToPdf(inPath);
        } else if (ext.equals("ppt") || ext.equals("pptx")) {
            pptToPdf(inPath, outPath);
        } else if (ext.equals("txt")) {
            txtToPdf(inPath, outPath);
        } else if (ext.equals("doc") || ext.equals("docx")) {
            wordToPdf(inPath, outPath);
        } else {
            log.error("不支持的文件类型：{}", ext);
            throw new RuntimeException("不支持的文件类型：" + ext);
        }

        return outPath;
    }

    public static void toPdf(String inPath, String outPath) {
        String outExt = outPath.substring(outPath.lastIndexOf(".") + 1).toLowerCase();
        if (!outExt.equals("pdf")) {
            log.error("输出文件类型必须为PDF");
            throw new RuntimeException("输出文件类型必须为PDF");
        }

        String inExt = inPath.substring(inPath.lastIndexOf(".") + 1).toLowerCase();
        if (inExt.equals("xls") || inExt.equals("xlsx")) {
            excelToPdf(inPath, outPath);
        } else if (inExt.equals("ppt") || inExt.equals("pptx")) {
            pptToPdf(inPath, outPath);
        } else if (inExt.equals("txt")) {
            txtToPdf(inPath, outPath);
        } else if (inExt.equals("doc") || inExt.equals("docx")) {
            wordToPdf(inPath, outPath);
        } else {
            log.error("不支持的文件类型：{}", inExt);
            throw new RuntimeException("不支持的文件类型：" + inExt);
        }
    }

    /**
     * Word文档转PDF
     *
     * @param inPath Word文档路径
     * @return PDF文件路径
     */
    public static String wordToPdf(String inPath) {
        String outPath = inPath.substring(0, inPath.lastIndexOf(".")) + ".pdf";
        wordToPdf(inPath, outPath);
        return outPath;
    }

    /**
     * Word文档转PDF
     *
     * @param inPath  Word文档路径
     * @param outPath 输出PDF路径
     */
    public static void wordToPdf(String inPath, String outPath) {
        if (!getLicense()) {
            log.error("Word转PDF失败：许可证验证失败");
            return;
        }

        FileOutputStream os = null;
        try {
            long startTime = System.currentTimeMillis();
            File file = new File(outPath);
            os = new FileOutputStream(file);

            // 在Linux环境下设置字体目录
            if (isLinux()) {
                FontSettings.getDefaultInstance().setFontsFolder("/usr/share/fonts/chinese", true);
            }

            Document doc = new Document(inPath);

            // 添加保存选项
            com.aspose.words.PdfSaveOptions saveOptions = new com.aspose.words.PdfSaveOptions();
            // 降低图像质量以减少内存使用
            saveOptions.setJpegQuality(80);
            // 使用内存优化模式
            saveOptions.setMemoryOptimization(true);

            doc.save(os, saveOptions);

            long endTime = System.currentTimeMillis();
            log.info("Word转PDF成功，耗时：{} 秒", ((endTime - startTime) / 1000.0));
        } catch (Exception e) {
            log.error("Word转PDF失败：{}", e.getMessage(), e);
        } finally {
            if (os != null) {
                try {
                    os.flush();
                    os.close();
                } catch (IOException e) {
                    log.error("关闭输出流失败：{}", e.getMessage(), e);
                }
            }
        }
    }

    /**
     * 文本文件转PDF
     *
     * @param inPath 文本文件路径
     * @return PDF文件路径
     */
    public static String txtToPdf(String inPath) {
        String outPath = inPath.substring(0, inPath.lastIndexOf(".")) + ".pdf";
        txtToPdf(inPath, outPath);
        return outPath;
    }

    /**
     * 文本文件转PDF
     *
     * @param inPath  文本文件路径
     * @param outPath 输出PDF路径
     */
    public static void txtToPdf(String inPath, String outPath) {
        if (!getLicense()) {
            log.error("文本转PDF失败：许可证验证失败");
            return;
        }

        FileOutputStream os = null;
        try {
            long startTime = System.currentTimeMillis();
            File file = new File(outPath);
            os = new FileOutputStream(file);

            Document doc = new Document();
            doc.removeAllChildren(); // 移除所有现有内容

            // 读取文本文件内容
            String text = new String(Files.readAllBytes(Paths.get(inPath)), StandardCharsets.UTF_8);

            // 将文本内容添加到文档
            com.aspose.words.DocumentBuilder builder = new com.aspose.words.DocumentBuilder(doc);
            builder.writeln(text);

            // 保存为PDF
            doc.save(os, SaveFormat.PDF);

            long endTime = System.currentTimeMillis();
            log.info("文本转PDF成功，耗时：{} 秒", ((endTime - startTime) / 1000.0));
        } catch (Exception e) {
            log.error("文本转PDF失败：{}", e.getMessage(), e);
        } finally {
            if (os != null) {
                try {
                    os.flush();
                    os.close();
                } catch (IOException e) {
                    log.error("关闭输出流失败：{}", e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Excel文件转PDF
     *
     * @param inPath Excel文件路径
     * @return PDF文件路径
     */
    public static String excelToPdf(String inPath) {
        String outPath = inPath.substring(0, inPath.lastIndexOf(".")) + ".pdf";
        excelToPdf(inPath, outPath);
        return outPath;
    }

    /**
     * Excel文件转PDF
     *
     * @param inPath  Excel文件路径
     * @param outPath 输出PDF路径
     */
    public static void excelToPdf(String inPath, String outPath) {
        if (!getLicense1()) {
            log.error("Excel转PDF失败：许可证验证失败");
            return;
        }

        FileOutputStream os = null;
        try {
            long startTime = System.currentTimeMillis();
            File file = new File(outPath);
            os = new FileOutputStream(file);

            Workbook wb = new Workbook(inPath);
            PdfSaveOptions pdfSaveOptions = new PdfSaveOptions();
            pdfSaveOptions.setOnePagePerSheet(true);

            wb.save(os, com.aspose.cells.SaveFormat.PDF);

            long endTime = System.currentTimeMillis();
            log.info("Excel转PDF成功，耗时：{} 秒", ((endTime - startTime) / 1000.0));
        } catch (Exception e) {
            log.error("Excel转PDF失败：{}", e.getMessage(), e);
        } finally {
            if (os != null) {
                try {
                    os.flush();
                    os.close();
                } catch (IOException e) {
                    log.error("关闭输出流失败：{}", e.getMessage(), e);
                }
            }
        }
    }

    /**
     * PPT文件转PDF
     *
     * @param inPath PPT文件路径
     * @return PDF文件路径
     */
    public static String pptToPdf(String inPath) {
        String outPath = inPath.substring(0, inPath.lastIndexOf(".")) + ".pdf";
        pptToPdf(inPath, outPath);
        return outPath;
    }

    /**
     * PPT文件转PDF
     *
     * @param inPath  PPT文件路径
     * @param outPath 输出PDF路径
     */
    public static void pptToPdf(String inPath, String outPath) {
        if (!getLicense2()) {
            log.error("PPT转PDF失败：许可证验证失败");
            return;
        }

        FileInputStream fileInput = null;
        FileOutputStream os = null;
        try {
            long startTime = System.currentTimeMillis();
            fileInput = new FileInputStream(inPath);
            os = new FileOutputStream(outPath);

            com.aspose.slides.Presentation pres = new com.aspose.slides.Presentation(fileInput);
            pres.save(os, com.aspose.slides.SaveFormat.Pdf);

            long endTime = System.currentTimeMillis();
            log.info("PPT转PDF成功，耗时：{} 秒", ((endTime - startTime) / 1000.0));
        } catch (Exception e) {
            log.error("PPT转PDF失败：{}", e.getMessage(), e);
        } finally {
            try {
                if (fileInput != null) {
                    fileInput.close();
                }
                if (os != null) {
                    os.flush();
                    os.close();
                }
            } catch (IOException e) {
                log.error("关闭输入/输出流失败：{}", e.getMessage(), e);
            }
        }
    }

    /**
     * 判断当前系统是否为Linux
     */
    private static boolean isLinux() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("linux");
    }

    /**
     * 获取Word和文本转换的许可证
     */
    public static boolean getLicense() {
        boolean result = false;
        try {
            InputStream is = PdfUtil.class.getClassLoader().getResourceAsStream("license.xml");
            License aposeLic = new License();
            aposeLic.setLicense(is);
            result = true;
        } catch (Exception e) {
            log.error("获取Word许可证失败：{}", e.getMessage(), e);
        }
        return result;
    }

    /**
     * 获取Excel转换的许可证
     */
    public static boolean getLicense1() {
        boolean result = false;
        try {
            InputStream is = PdfUtil.class.getClassLoader().getResourceAsStream("license.xml");
            com.aspose.cells.License aposeLic = new com.aspose.cells.License();
            aposeLic.setLicense(is);
            result = true;
        } catch (Exception e) {
            log.error("获取Excel许可证失败：{}", e.getMessage(), e);
        }
        return result;
    }

    /**
     * 获取PPT转换的许可证
     */
    public static boolean getLicense2() {
        boolean result = false;
        try {
            InputStream is = PdfUtil.class.getClassLoader().getResourceAsStream("license.xml");
            com.aspose.slides.License aposeLic = new com.aspose.slides.License();
            aposeLic.setLicense(is);
            result = true;
        } catch (Exception e) {
            log.error("获取PPT许可证失败：{}", e.getMessage(), e);
        }
        return result;
    }

    /**
     * 将MultipartFile文件转换为PDF并写入输出流
     *
     * @param file         上传的文件
     * @param outputStream 输出流
     * @return 转换成功返回true，失败返回false
     */
    public static boolean convertToPdf(MultipartFile file, OutputStream outputStream) {
        if (file == null || file.isEmpty()) {
            log.error("文件为空，无法转换为PDF");
            return false;
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            log.error("无法获取文件名，无法转换为PDF");
            return false;
        }

        // 获取文件扩展名
        String ext = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();

        try {
            // 获取文件字节数组
            byte[] fileBytes = file.getBytes();

            // 根据文件类型调用不同的转换方法
            if (ext.equals("xls") || ext.equals("xlsx")) {
                return excelToPdf(fileBytes, outputStream);
            } else if (ext.equals("ppt") || ext.equals("pptx")) {
                return pptToPdf(fileBytes, outputStream);
            } else if (ext.equals("txt")) {
                return txtToPdf(fileBytes, outputStream);
            } else if (ext.equals("doc") || ext.equals("docx")) {
                return wordToPdf(fileBytes, outputStream);
            } else if (ext.equals("pdf")) {
                // 如果已经是PDF，直接写入输出流
                outputStream.write(fileBytes);
                outputStream.flush();
                return true;
            } else {
                log.error("不支持的文件类型：{}", ext);
                return false;
            }
        } catch (IOException e) {
            log.error("文件处理失败：{}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Excel字节数组转PDF
     *
     * @param fileBytes    Excel文件字节数组
     * @param outputStream 输出流
     * @return 转换成功返回true，失败返回false
     */
    public static boolean excelToPdf(byte[] fileBytes, OutputStream outputStream) {
        if (!getLicense1()) {
            log.error("Excel转PDF失败：许可证验证失败");
            return false;
        }

        try {
            long startTime = System.currentTimeMillis();

            ByteArrayInputStream inputStream = new ByteArrayInputStream(fileBytes);
            Workbook wb = new Workbook(inputStream);
            PdfSaveOptions pdfSaveOptions = new PdfSaveOptions();
            pdfSaveOptions.setOnePagePerSheet(true);

            wb.save(outputStream, com.aspose.cells.SaveFormat.PDF);

            long endTime = System.currentTimeMillis();
            log.info("Excel转PDF成功，耗时：{} 秒", ((endTime - startTime) / 1000.0));
            return true;
        } catch (Exception e) {
            log.error("Excel转PDF失败：{}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Word字节数组转PDF
     *
     * @param fileBytes    Word文件字节数组
     * @param outputStream 输出流
     * @return 转换成功返回true，失败返回false
     */
    public static boolean wordToPdf(byte[] fileBytes, OutputStream outputStream) {
        if (!getLicense()) {
            log.error("Word转PDF失败：许可证验证失败");
            return false;
        }

        try {
            long startTime = System.currentTimeMillis();

            // 在Linux环境下设置字体目录
            if (isLinux()) {
                FontSettings.getDefaultInstance().setFontsFolder("/usr/share/fonts/chinese", true);
            }

            ByteArrayInputStream inputStream = new ByteArrayInputStream(fileBytes);
            Document doc = new Document(inputStream);

            // 添加保存选项
            com.aspose.words.PdfSaveOptions saveOptions = new com.aspose.words.PdfSaveOptions();
            // 降低图像质量以减少内存使用
            saveOptions.setJpegQuality(80);
            // 使用内存优化模式
            saveOptions.setMemoryOptimization(true);

            doc.save(outputStream, saveOptions);

            long endTime = System.currentTimeMillis();
            log.info("Word转PDF成功，耗时：{} 秒", ((endTime - startTime) / 1000.0));
            return true;
        } catch (Exception e) {
            log.error("Word转PDF失败：{}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * TXT字节数组转PDF
     *
     * @param fileBytes    TXT文件字节数组
     * @param outputStream 输出流
     * @return 转换成功返回true，失败返回false
     */
    public static boolean txtToPdf(byte[] fileBytes, OutputStream outputStream) {
        if (!getLicense()) {
            log.error("文本转PDF失败：许可证验证失败");
            return false;
        }

        try {
            long startTime = System.currentTimeMillis();

            Document doc = new Document();
            doc.removeAllChildren(); // 移除所有现有内容

            // 读取文本文件内容
            String text = new String(fileBytes, StandardCharsets.UTF_8);

            // 将文本内容添加到文档
            com.aspose.words.DocumentBuilder builder = new com.aspose.words.DocumentBuilder(doc);
            builder.writeln(text);

            // 保存为PDF
            doc.save(outputStream, SaveFormat.PDF);

            long endTime = System.currentTimeMillis();
            log.info("文本转PDF成功，耗时：{} 秒", ((endTime - startTime) / 1000.0));
            return true;
        } catch (Exception e) {
            log.error("文本转PDF失败：{}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * PPT字节数组转PDF
     *
     * @param fileBytes    PPT文件字节数组
     * @param outputStream 输出流
     * @return 转换成功返回true，失败返回false
     */
    public static boolean pptToPdf(byte[] fileBytes, OutputStream outputStream) {
        if (!getLicense2()) {
            log.error("PPT转PDF失败：许可证验证失败");
            return false;
        }

        try {
            long startTime = System.currentTimeMillis();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(fileBytes);

            com.aspose.slides.Presentation pres = new com.aspose.slides.Presentation(inputStream);
            pres.save(outputStream, com.aspose.slides.SaveFormat.Pdf);

            long endTime = System.currentTimeMillis();
            log.info("PPT转PDF成功，耗时：{} 秒", ((endTime - startTime) / 1000.0));
            return true;
        } catch (Exception e) {
            log.error("PPT转PDF失败：{}", e.getMessage(), e);
            return false;
        }
    }
}