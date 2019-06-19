package cn.threedr3am.awd;

import cn.threedr3am.awd.bean.FileInfo;
import cn.threedr3am.awd.utils.MD5Util;
import cn.threedr3am.awd.utils.StreamUtil;

import com.sun.javafx.scene.shape.PathUtils;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author xuanyh
 * @create 2018-12-01 10:08
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
        //检查源、目标目录合法性
        boolean checkDirResult = checkDir(sourceDir, targetDir);
        if (!checkDirResult)
            System.exit(1);
        //两个线程处理，一个防止修改、新增，一个防止删除
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        Map<String, FileInfo> sourceFileInfo = null;
        try {
            //读取源镜像目录内容，生成保护文档缓存
            log.info("------------------------ read source image info begin");
            sourceFileInfo = readSourceFileInfo(sourceDir);
            log.info("------------------------ read source image info end");
        } catch (IOException e) {
            e.printStackTrace();
        }
        log.info("running..............................");
        Map<String, FileInfo> finalSourceFileInfo = sourceFileInfo;
        executorService.execute(() -> {
            while (true) {
                try {
                    //目标目录遍历，防止被文件、文件夹修改、新增
                    protectedTargetDir(finalSourceFileInfo, targetDir);
                    Thread.sleep(1 * 1000);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        executorService.execute(() -> {
            while (true) {
                try {
                    //根据源目录镜像，检查目标目录丢失文件，并恢复
                    checkLostFile(finalSourceFileInfo, sourceDir, targetDir);
                    Thread.sleep(1 * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 检查目标目录内丢失的文件、目录并恢复
     *
     * @param sourceFileInfo
     * @param sourceDir
     * @param targetDir
     * @throws IOException
     */
    private static void checkLostFile(Map<String, FileInfo> sourceFileInfo, File sourceDir, File targetDir) {
        //遍历源镜像目录文档
        for (Map.Entry<String, FileInfo> entry : sourceFileInfo.entrySet()) {
            FileInfo fileInfo = entry.getValue();
            //目标目录中目标文件路径
            String targetFileUrl = targetDir.getAbsolutePath() + fileInfo.getUrl().replace(sourceDir.getAbsolutePath(), "");
            File file = new File(targetFileUrl);
            //不存在则意味被删除，需要进行恢复，目录则新建，文件则写入备份
            if (!file.exists()) {
                if (fileInfo.isFile()) {
                    try {
                        Files.copy(Paths.get(fileInfo.getUrl()),Paths.get(targetFileUrl));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
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
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                File readFile = dir.toFile();
                if (readFile.exists()) {
                    String path = readFile.getAbsolutePath().replace(targetDir.getAbsolutePath(), "");
                    FileInfo sourceFileInfo = sourceFileInfoMap.get(path);
                    //源镜像目录不存在该目录，则删除
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
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                File readFile = file.toFile();
                if (readFile.exists()) {
                    String path = readFile.getAbsolutePath().replace(targetDir.getAbsolutePath(), "");
                    FileInfo sourceFileInfo = sourceFileInfoMap.get(path);
                    //源镜像目录不存在该文件，则删除
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
                        String md5 = MD5Util.getMD5(readFile);
                        //检查目标文件md5是否和源镜像文件md5一致，不一致则删除并恢复
                        if (!sourceFileInfo.getMd5().equals(md5)) {
                            if (readFile.canWrite()) {
                                if (readFile.delete()) {
                                    byte[] sourceFileBytes = StreamUtil.readBytes(new File(sourceFileInfo.getUrl()));

                                    if (sourceFileBytes != null) {
                                        StreamUtil.writeBytes(file.toFile(), sourceFileBytes);
                                        log.info("删除&重写异常文件：" + readFile.getAbsolutePath());
                                    }
                                } else {
                                    log.info("删除&重写异常文件：" + readFile.getAbsolutePath() + " 失败");
                                }
                            } else {
                                log.info("异常文件：" + readFile.getAbsolutePath() + " 不可写");
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
                //源镜像目录备份
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
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                File readFile = file.toFile();
                //源镜像文件备份
                if (readFile.exists()) {
                    String realFileName = readFile.getAbsolutePath().replace(sourceDir.getAbsolutePath(), "");
                    if (readFile.isFile()) {
                        String md5 = MD5Util.getMD5(readFile);

                        FileInfo fileInfo = new FileInfo();
                        fileInfo.setMd5(md5);
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
        //源镜像目录需拥有读权限
        if (!sourceDir.isDirectory() || !sourceDir.canRead()) {
            log.log(Level.SEVERE, "路径指向：" + sourceDir.getAbsolutePath() + "非目录或不可读");
            return false;
        }
        //目标保护目录需有读写权限
        if (!targetDir.isDirectory() || !targetDir.canRead() || !targetDir.canWrite()) {
            log.log(Level.SEVERE, "路径指向：" + targetDir.getAbsolutePath() + "非目录或不可读、写");
            return false;
        }
        return true;
    }
}
