package cn.threedr3am.awd;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author met3d
 * @create 2018-12-01 14:18
 **/
public class APP {
    private static Logger log = Logger.getLogger(ProtectFile.class.toString());

    static {
        log.setLevel(Level.INFO);
    }

    public static void main(String[] args) {
        if (args.length != 2 || args[0].length() == 0 || args[1].length() == 0)
            log.log(Level.SEVERE, "参数错误，正确使用方法，例：java -jar file-protected.jar /Users/met3d/var/www/html /var/www/html");
        ProtectFile.execute(args[0], args[1]);
    }
}
