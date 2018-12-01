package cn.threedr3am.awd;

import cn.threedr3am.awd.bean.FileInfo;
import cn.threedr3am.awd.utils.MD5Util;

import java.io.*;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * by threedr3am (2018-12-01)
 */
public class ProtectFile {
    private static Logger log = Logger.getLogger(ProtectFile.class.toString());

    static {
        log.setLevel(Level.INFO);
    }

    /**
     * 执行目录监控保护
     *
     * @param sourceDirUrl
     * @param targetDirurl
     */
    public static void execute(String sourceDirUrl, String targetDirurl) {
        File sourceDir = new File(sourceDirUrl);
        File targetDir = new File(targetDirurl);
        boolean checkDirResult = checkDir(sourceDir, targetDir);
        if (!checkDirResult)
            System.exit(1);
        try {
            Map<String, FileInfo> sourceFileInfo = readSourceFileInfo(sourceDir);
            while (true) {
                protectedTargetDir(sourceFileInfo, targetDir);
                checkLostFile(sourceFileInfo, sourceDir, targetDir);
                Thread.sleep(1 * 1000);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 检查目标目录内丢失的文件、目录并恢复
     *
     * @param sourceFileInfo
     * @param sourceDir
     * @param targetDir
     * @throws IOException
     */
    private static void checkLostFile(Map<String, FileInfo> sourceFileInfo, File sourceDir, File targetDir) throws IOException {
        for (Map.Entry<String, FileInfo> entry : sourceFileInfo.entrySet()) {
            FileInfo fileInfo = entry.getValue();
            String targetFileUrl = targetDir.getAbsolutePath() + fileInfo.getUrl().replace(sourceDir.getAbsolutePath(), "");
            File file = new File(targetFileUrl);
            if (!file.exists()) {
                if (fileInfo.isFile()) {
                    BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(fileInfo.getUrl()));
                    byte[] bytes = new byte[bufferedInputStream.available()];
                    bufferedInputStream.read(bytes);
                    bufferedInputStream.close();
                    BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(file));
                    bufferedOutputStream.write(bytes);
                    bufferedOutputStream.close();
                    log.info("写入被删除文件：" + targetFileUrl);
                } else {
                    file.mkdir();
                    log.info("写入被删除目录：" + targetFileUrl);
                }
            }
        }
    }


    /**
     * 保护目标目录
     *
     * @param sourceFileInfoMap
     * @param targetDir
     * @throws IOException
     */
    private static void protectedTargetDir(final Map<String, FileInfo> sourceFileInfoMap, final File targetDir) throws IOException {
        Files.walkFileTree(targetDir.toPath(), new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                File readFile = dir.toFile();
                if (readFile.exists()) {
                    String path = readFile.getAbsolutePath().replace(targetDir.getAbsolutePath(), "");
                    FileInfo sourceFileInfo = sourceFileInfoMap.get(path);
                    if (sourceFileInfo == null) {
                        if (readFile.canWrite()) {
                            if (readFile.delete()) {
                                log.info("删除异常目录：" + readFile.getAbsolutePath());
                            } else {
                                log.info("删除异常目录：" + readFile.getAbsolutePath() + " 失败");
                            }
                        } else {
                            log.info("异常目录：" + readFile.getAbsolutePath() + " 不可写");
                        }
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                File readFile = file.toFile();
                if (readFile.exists()) {
                    String path = readFile.getAbsolutePath().replace(targetDir.getAbsolutePath(), "");
                    FileInfo sourceFileInfo = sourceFileInfoMap.get(path);
                    if (readFile.isFile()) {
                        if (sourceFileInfo == null) {
                            if (readFile.canWrite()) {
                                if (readFile.delete()) {
                                    log.info("删除异常文件：" + readFile.getAbsolutePath());
                                } else {
                                    log.info("删除异常文件：" + readFile.getAbsolutePath() + " 失败");
                                }
                            } else {
                                log.info("异常文件：" + readFile.getAbsolutePath() + " 不可写");
                            }
                        } else {
                            BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(readFile));
                            byte[] bytes = new byte[bufferedInputStream.available()];
                            bufferedInputStream.read(bytes);
                            String md5 = MD5Util.getMD5(bytes);
                            bufferedInputStream.close();
                            if (!sourceFileInfo.getMd5().equals(md5)) {
                                if (readFile.canWrite()) {
                                    if (readFile.delete()) {
                                        bufferedInputStream = new BufferedInputStream(new FileInputStream(sourceFileInfo.getUrl()));
                                        bytes = new byte[bufferedInputStream.available()];
                                        bufferedInputStream.read(bytes);
                                        bufferedInputStream.close();
                                        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(file.toFile()));
                                        bufferedOutputStream.write(bytes);
                                        bufferedOutputStream.close();
                                        log.info("删除&重写异常文件：" + readFile.getAbsolutePath());
                                    } else {
                                        log.info("删除&重写异常文件：" + readFile.getAbsolutePath() + " 失败");
                                    }
                                } else {
                                    log.info("异常文件：" + readFile.getAbsolutePath() + " 不可写");
                                }
                            }
                        }
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * 读取源镜像目录，即备份目录信息
     *
     * @param sourceDir
     * @return
     * @throws IOException
     */
    private static Map<String, FileInfo> readSourceFileInfo(final File sourceDir) throws IOException {
        final Map<String, FileInfo> fileInfoMap = new TreeMap<>();
        Files.walkFileTree(sourceDir.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                File readFile = dir.toFile();
                if (readFile.exists()) {
                    String realFileName = readFile.getAbsolutePath().replace(sourceDir.getAbsolutePath(), "");
                    FileInfo fileInfo = new FileInfo();
                    fileInfo.setUrl(readFile.getAbsolutePath());
                    fileInfo.setMd5(MD5Util.getMD5(realFileName.getBytes()));
                    fileInfoMap.put(realFileName, fileInfo);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                File readFile = file.toFile();
                if (readFile.exists()) {
                    String realFileName = readFile.getAbsolutePath().replace(sourceDir.getAbsolutePath(), "");
                    if (readFile.isFile()) {
                        BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(readFile));
                        byte[] bytes = new byte[bufferedInputStream.available()];
                        bufferedInputStream.read(bytes);
                        FileInfo fileInfo = new FileInfo();
                        fileInfo.setMd5(MD5Util.getMD5(bytes));
                        fileInfo.setUrl(readFile.getAbsolutePath());
                        fileInfo.setFile(true);
                        fileInfoMap.put(realFileName, fileInfo);
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return fileInfoMap;
    }

    /**
     * 检查目录是否存在
     *
     * @param sourceDir
     * @param targetDir
     * @return
     */
    private static boolean checkDir(File sourceDir, File targetDir) {
        if (!sourceDir.isDirectory() || !sourceDir.canRead()) {
            log.log(Level.SEVERE, "路径指向：" + sourceDir.getAbsolutePath() + "非目录或不可读");
            return false;
        }
        if (!targetDir.isDirectory() || !targetDir.canRead() || !targetDir.canWrite()) {
            log.log(Level.SEVERE, "路径指向：" + targetDir.getAbsolutePath() + "非目录或不可读、写");
            return false;
        }
        return true;
    }
}
