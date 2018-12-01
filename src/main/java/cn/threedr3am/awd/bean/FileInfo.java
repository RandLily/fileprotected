package cn.threedr3am.awd.bean;

/**
 * @author xuanyh
 * @create 2018-12-01 10:16
 **/
public class FileInfo {
    //源文件md5
    private String md5;
    //源文件url
    private String url;
    //是否文件
    private boolean isFile;

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isFile() {
        return isFile;
    }

    public void setFile(boolean file) {
        isFile = file;
    }
}
