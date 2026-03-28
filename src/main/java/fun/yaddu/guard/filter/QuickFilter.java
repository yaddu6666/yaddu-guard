package fun.yaddu.guard.filter;

import java.util.*;

public class QuickFilter {

    private static final String[] ROOTS = {
        // BSDK family
        "bsdk","bhosdk","bhosdike","bhosdiki","bhosdiwale","bhosadi","bhosdika","bhsdke",
        // MC family
        "mc","madarchod","maderchod","madarchodd","madarchood","madarchodi","maadarchodd",
        // BC family
        "bc","behenchod","behanchod","behnchod","bhenchodd","bhenchodi","bhnchd",
        // Chut family
        "chutiya","chutia","choot","chut","chutiye","chutiyo","chootiya","chutiyap","chutiyagiri","chutmarika",
        // Lund/Loda
        "lund","loda","lode","lodey","lavda","lawda","lavde","lawde","lundbaaz","lundlelo",
        // Randi
        "randi","raand","rande","randibaaz","randibazi","randikhana","gashti","kasbi","rakhail",
        // Gaand
        "gaand","gand","gaandu","gandu","ganduu","gaandmara","gandmara","gaandufaad","gaandphati",
        // Harami
        "harami","haramzada","haramzadi","haramipan","haraami","haraamzada","haramkhor","haramjaada",
        // Sala/Saala
        "sala","saala","saale","saali","saalon","saalebaap",
        // Kamina
        "kamina","kamini","kameena","kameeni","kaminey","kaminagi","kaminapan",
        // Kutte
        "kutte","kutta","kutti","kuttiya","kutton","kuttekamage",
        // Suar
        "suar","suvar","suwar","suarke","suarkabacha",
        // Chod family
        "chod","chodna","choda","chodi","chodd","chodmara","chodmaara","chodbaz","chodbazi","chodke",
        // Jhant
        "jhant","jhantoo","jhantu","jhaant","jhantoke",
        // Bhadwa
        "bhadwa","bhadwe","bhadwaa","bhadwi","bhadwagiri",
        // Gandu variations
        "phuddu","fuddu","fuddoo","phuddi","fuddi",
        // Dalaal
        "dalaal","dalal","dalaali",
        // Hijra
        "chakka","hijda","hijra","hijraa","meetha",
        // Ullu
        "ullu","ulluka","ullubanao","ullubanaoge",
        // Gadha
        "gadha","gadhaa","gadhon","gadhekabacha",
        // BKL
        "bkl","bklod","bkloda","bkllod","bkloke",
        // MKC
        "mkc","mkcl","mkl","mkcwala",
        // Maa ki
        "terimaaki","maaki","terimaaki","baapmara","terimaaka","terimaake",
        // Nalayak
        "nalayak","nikamma","nikammi","nalayakkahin",
        // Bhojpuri
        "launda","laundebaaz","laundebaazi","raurahi","rauraha",
        // Punjabi
        "maadadi","penchod","pencho","land","lann","bhainchod","bhain","bhainchodd",
        // Jhatu
        "jhatu","jhatuu","jhaatu",
        // Taklu
        "taklu","taklaa","andha","andhaa",
        // Mote
        "motahaathi","motesuar",
        // Besharam
        "besharam","besharami","besharaam",
        // Naalayak
        "naalayak","naalaayak",
        // Teri aukat
        "teriaukaat","teriaukat","aukaatnahi",
        // Ghatiya
        "ghatiya","ghatiyaa","ghatiyapan",
        // Bakwas
        "bakwaas","bkwas","bakwaasi","bakwason",
        // Chhinal
        "chhinal","chhinaal","chhinali",
        // Haraami
        "haraami","haraamkhor",
        // Paagal
        "paagalkahin","pagalkahin","paagalkuta",
        // Saand
        "saand","saandkabacha",
        // ENGLISH
        "fuck","fuk","fuuck","fck","fcuk","effyou","fugk","phuck",
        "shit","sht","shiit","shitt","bs",
        "bitch","biitch","bytch","b1tch","biotch",
        "bastard","baztard","b4stard","bastad",
        "ass","arse","azz","a55",
        "cunt","kunt","c0nt",
        "dick","dik","diick","d1ck",
        "pussy","pussi","pu55y",
        "whore","hoe","wh0re",
        "nigga","nigger","n1gga","niga",
        "retard","retarded","ret4rd",
        "motherfucker","motherf","mf","mofo",
        "sonofabitch","sonofa","sob",
        "jackass","asshole","azzhole","a55hole",
        "prick","pr1ck",
        "slut","slutt","sl0t",
        "dumbass","dumba","dumbazz",
        "idiot","idi0t","idiott",
        "stupid","stup1d",
        "moron","mor0n",
        "loser","l0ser","looser",
        "jerk","douchebag","douche",
        "screw","screwu","screwoff",
        "wtf","stfu","gtfo",
        // Hinglish mix
        "madarfucker","madafaka","madafacker","madafaka",
        "benchod","bhenchod",
        "bhosadike","bhosadiki",
        "chootiyo","chutiyao",
        "gaalimat","galiman"
    };

    private static final Set<String> NORMALIZED;
    static {
        Set<String> tmp = new HashSet<>();
        for (String r : ROOTS) tmp.add(normalize(r));
        NORMALIZED = Collections.unmodifiableSet(tmp);
    }

    public static String normalize(String msg) {
        if (msg == null) return "";
        String s = msg.toLowerCase();
        s = s.replace("@","a").replace("4","a")
             .replace("3","e").replace("1","i")
             .replace("!","i").replace("0","o")
             .replace("5","s").replace("$","s")
             .replace("7","t").replace("+","t")
             .replace("8","b").replace("6","g")
             .replace("9","g").replace("2","z")
             .replace("|","i").replace("(","c")
             .replace("/","l").replace("\\","l");
        s = s.replaceAll("[^a-z]","");
        return s;
    }

    public static boolean mightBeToxic(String message) {
        if (message == null || message.trim().isEmpty()) return false;
        String norm = normalize(message);
        if (norm.length() < 2) return false;
        // Direct match
        if (NORMALIZED.contains(norm)) return true;
        // Substring match
        for (String root : NORMALIZED) {
            if (root.length() >= 3 && norm.contains(root)) return true;
        }
        return false;
    }
}
