package com.minipay.payment.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class QRCodeUtil {
    
    /**
     * 生成二维码Base64
     */
    public static String generateQRCodeBase64(String content, int width, int height) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            // 指定二维码内容编码格式
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            // 二维码白边边距
            hints.put(EncodeHintType.MARGIN, 1);

            // 按照配置、尺寸，把文本内容编译成比特矩阵，BarcodeFormat.QR_CODE指定二维码，不是条形码
            BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height, hints);

            // 将比特矩阵写入字节数组输出流，生成 PNG 图片，MatrixToImageWriter：ZXing 工具，把黑白点阵矩阵 → 转换成真实 PNG 图片二进制数据
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", outputStream);

            // 取出内存中 PNG 图片的原始字节数组
            byte[] bytes = outputStream.toByteArray();
            // 将图片字节数组转为 Base64 编码字符串+拼接前缀 data:image/png;base64,
            //这是浏览器识别 Base64 图片的固定格式，前端不用额外处理，直接赋值给 img 的 src 属性即可渲染图片。
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
            
        } catch (WriterException | IOException e) {
            throw new RuntimeException("生成二维码失败", e);
        }
    }
}
