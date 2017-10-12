package util;
import java.text.Normalizer;
import java.util.regex.Pattern; 

public class RemoveAcentos {
	public static String removerAcentos(String nome){  
		String nfdNormalizedString = Normalizer.normalize(nome, Normalizer.Form.NFD); 
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(nfdNormalizedString).replaceAll("");
	}
	
	public static void main(String args[]) throws Exception{
		System.out.println(removerAcentos("À Á Â Ã Ä Å Æ Ç È É Ê Ë Ì Í Î Ï Ð Ñ Ò Ó Ô Õ Ö Ø Ù Ú Û Ü Ý Þ ß à á â ã ä å æ ç è é ê ë ì í î ï ð ñ ò ó ô õ ö ø ù ú û ü ý þ ÿ "));
	}
}
