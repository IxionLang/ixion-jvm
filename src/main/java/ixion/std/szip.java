package ixion.std;



import ixion.std.szip_utils.utils.Crc32;
import ixion.std.szip_utils.utils.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class szip {

    private static final byte[] localFileHeaderSignData = new byte[]{0x50, 0x4b, 0x03, 0x04};
    private static final byte[] centralFileHeaderSignData = new byte[]{0x50, 0x4b, 0x01, 0x02};
    private static final byte[] endFileHeaderSignData = new byte[]{0x50, 0x4b, 0x05, 0x06};


    private static final byte[] versionData = new byte[]{0x14, 0x00};
    private static final byte[] purposeBitFlagData = new byte[]{0x02, 0x00};
    private static final byte[] compressionMethodData = new byte[]{0x00, 0x00};

    private szip(){}

    public static void archive(String destFile, String... files) throws IOException {

        Map<String, Integer> crcMap = prepareCrc(files);

        FileOutputStream outputFile = new FileOutputStream(destFile);

        int[] offsets = new int[files.length];
        int offset = 0;

        byte[] b = new byte[65536];
        for (int i = 0; i < files.length; i++) {
            String filename = files[i];
            File file = new File(filename);

            byte[] localHeaderData = writeLocalHeader(file, crcMap);
            long fileSize = file.length();
            offsets[i] = offset;
            offset += localHeaderData.length + fileSize;
            outputFile.write(localHeaderData);

            FileInputStream inputStream = new FileInputStream(file);
            int c;
            while ((c = inputStream.read(b)) != -1) {
                outputFile.write(b, 0, c);
            }

            inputStream.close();
        }
        //
        int centralDirSize = 0;
        int centralDirOffset = offset;
        for (int i = 0; i < files.length; i++) {
            String filename = files[i];
            File file = new File(filename);

            byte[] centralHeaderData = writeCentralHeader(file, offsets[i], crcMap);
            centralDirSize += centralHeaderData.length;

            outputFile.write(centralHeaderData);
        }
        //
        byte[] endHeaderData = writeEndHeader(centralDirSize, centralDirOffset, files.length);
        outputFile.write(endHeaderData);
        //

        outputFile.close();
    }

    private static Map<String, Integer> prepareCrc(String... files) throws IOException {
        Map<String, Integer> result = new HashMap<>();

        for (String filename : files) {
            File file = new File(filename);
            result.put(file.getAbsolutePath(), Crc32.getCrc(file));
        }

        return result;
    }

    private static byte[] writeLocalHeader(File file, Map<String, Integer> crcMap) throws IOException {
        String filename = file.getName();

        byte[] dateData = Utils.toByteArray(Utils.getDate(file.lastModified()), 2);
        byte[] timeData = Utils.toByteArray(Utils.getTime(file.lastModified()), 2);
        byte[] crcData = Utils.toByteArray(crcMap.get(file.getAbsolutePath()), 4);
        byte[] compressedSizeData = Utils.toByteArray(file.length(), 4);
        byte[] fileSizeData = Utils.toByteArray(file.length(), 4);

        byte[] filenameLengthData = Utils.toLittleEndianByteArray(filename.length(), 2);
        byte[] filenameData = filename.getBytes();

        byte[] extraLengthData = new byte[]{0x00, 0x00};
        byte[] extraFieldData = new byte[]{};

        byte[] result = Utils.makeArray(localFileHeaderSignData, versionData, purposeBitFlagData,
                compressionMethodData, timeData, dateData, crcData, compressedSizeData, fileSizeData,
                filenameLengthData, extraLengthData, filenameData, extraFieldData);

        Utils.arraysCopy(result, localFileHeaderSignData, versionData, purposeBitFlagData,
                compressionMethodData, timeData, dateData, crcData, compressedSizeData, fileSizeData,
                filenameLengthData, extraLengthData, filenameData, extraFieldData);

        return result;
    }

    private static byte[] writeCentralHeader(File file, int offset, Map<String, Integer> crcMap) throws IOException {
        String filename = file.getName();

        byte[] dateData = Utils.toByteArray(Utils.getDate(file.lastModified()), 2);
        byte[] timeData = Utils.toByteArray(Utils.getTime(file.lastModified()), 2);
        byte[] crcData = Utils.toByteArray(crcMap.get(file.getAbsolutePath()), 4);
        byte[] compressedSizeData = Utils.toByteArray(file.length(), 4);
        byte[] fileSizeData = Utils.toByteArray(file.length(), 4);

        byte[] filenameLengthData = Utils.toLittleEndianByteArray(filename.length(), 2);
        byte[] filenameData = filename.getBytes();

        byte[] extraLengthData = new byte[]{0x00, 0x00};
        byte[] extraFieldData = new byte[]{};

        byte[] commentLengthData = new byte[]{0x00, 0x00};
        byte[] commentFieldData = new byte[]{};

        byte[] diskNumberData = new byte[]{0x00, 0x00};

        int internalFileAttr = 0;

        byte[] internalFileAttrsData = Utils.toByteArray(internalFileAttr, 2);
        byte[] externalFileAttrsData = new byte[]{0x00, 0x00, 0x00, 0x00};
        byte[] relativeOffsetAttrsData = Utils.toByteArray(offset, 4);

        byte[] result = Utils.makeArray(centralFileHeaderSignData, versionData, versionData,
                purposeBitFlagData,
                compressionMethodData, timeData, dateData, crcData,
                compressedSizeData, fileSizeData,
                filenameLengthData, extraLengthData,
                commentLengthData,
                diskNumberData, internalFileAttrsData, externalFileAttrsData, relativeOffsetAttrsData,
                filenameData, extraFieldData,
                commentFieldData);

        Utils.arraysCopy(result, centralFileHeaderSignData, versionData, versionData, purposeBitFlagData, compressionMethodData,
                timeData, dateData, crcData, compressedSizeData, fileSizeData, filenameLengthData, extraLengthData, commentLengthData,
                diskNumberData, internalFileAttrsData, externalFileAttrsData, relativeOffsetAttrsData, filenameData, extraFieldData, commentFieldData);

        return result;
    }

    private static byte[] writeEndHeader(int centralDir, int offsetCentral, int count) throws IOException {
        byte[] numberOnDiskData = new byte[]{0x00, 0x00};
        byte[] diskCentralDirData = new byte[]{0x00, 0x00};
        byte[] numberCentralDiskData = Utils.toByteArray(count, 2);
        byte[] totalNumberDirData = Utils.toByteArray(count, 2);
        byte[] sizeCentralDirData = Utils.toByteArray(centralDir, 4);
        byte[] offsetCentralDirData = Utils.toByteArray(offsetCentral, 4);

        byte[] commentLengthData = new byte[]{0x00, 0x00};
        byte[] commentFieldData = new byte[]{};

        byte[] result = Utils.makeArray(endFileHeaderSignData, numberOnDiskData, diskCentralDirData, numberCentralDiskData,
                totalNumberDirData, sizeCentralDirData, offsetCentralDirData, commentLengthData, commentFieldData);

        Utils.arraysCopy(result, endFileHeaderSignData, numberOnDiskData, diskCentralDirData, numberCentralDiskData, totalNumberDirData,
                sizeCentralDirData, offsetCentralDirData, commentLengthData, commentFieldData);

        return result;
    }
}
