package org.chernovia.lib.misc;

public class RandomNameGenerator {
	public static final String[] CONSONANTS = { 
		"b","bl","br",
		"c","ch","cl","cr",
		"d","dh","dr",
		"f","fl","fr",
		"g","gh","gl","gr",
		"h",
		"k",
		"l","ll",
		"m",
		"n",
		"p","pl","pr",
		"qu",
		"r",
		"s","sc","sh","sk","sl","sm","sn","sp","squ","sr","st","sw",
		"t","th","tr","tw",
		"v",
		"w","wh","wr",
		//"x",
		//"z"
	};
	public static final String[] FINISHERS = { "ck", "nk", "rk", "sk", "lt", "rm" };
	public static final String[] VOWELS = { "a","e","i","o","u"};
		
	public static String rndWord() {
		String word = "";
		int l = (int)(Math.random()*5) + 3;
		boolean vowel = true;
		while (word.length() < l) {
			if (!vowel) {
				word += rndVowel(); vowel = true;
			}
			else {
				if (flip(25)) word += rndVowel();
				else {
					word += rndConsonant(false); vowel = false;
				}
			}
		}
		if (vowel && flip(50)) word += rndConsonant(true);
		else {
			word += rndVowel();
			if (flip(50)) {
				String c = rndConsonant(false);
				while (c.length() != 1) { c = rndConsonant(false); }
				word += c;
			}
		}
		return word;
	}
	
	public static String rndName() {
		String firstName = rndWord();
		firstName = firstName.toUpperCase().substring(0, 1) + firstName.substring(1);
		String lastName = rndWord();
		lastName = lastName.toUpperCase().substring(0, 1) + lastName.substring(1);
		return firstName + " " + lastName;
	}
	
	public static String rndVowel() {
		return VOWELS[(int)(Math.random() * VOWELS.length)];
	}
	
	public static String rndConsonant(boolean finish) {
		if (!finish) return CONSONANTS[(int)(Math.random() * CONSONANTS.length)];
		else return FINISHERS[(int)(Math.random() * FINISHERS.length)];
	}
		
	public static boolean flip(int chance) {
		return (Math.random() < (chance/100f));
	}
	
	public static void test() {
		for (int i=0;i<50;i++) System.out.println(rndName());
	}
	
}


