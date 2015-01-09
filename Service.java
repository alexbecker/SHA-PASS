package com.alexbecker.sha_pass;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.util.Log;

public class Service {
	public String name;
	public int length;
	public boolean lowercase;
	public boolean uppercase;
	public boolean number;
	public boolean symbol;
	
	private static final char[] symbols = {'!', '@', '#', '$', '%', '^', '&', '*'};
	
	public String generate(String masterPassword) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(masterPassword.concat(" ").concat(this.name).concat("\n").getBytes());
			byte[] mdbytes = md.digest();
	        StringBuffer sb = new StringBuffer();
	        for (int i = 0; i < mdbytes.length; i++) {
	          sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
	        }
	        String shaSum = sb.toString();
			
			for (int i=0; i < 256 - this.length; i++) {
				String pass = shaSum.substring(i, i + this.length - 1);

				boolean satisfiesLowercase = !this.lowercase || !pass.toUpperCase().equals(pass);
				boolean satisfiesUppercase = !this.uppercase || pass.replaceAll("[0-9]", "").length() > 1;
				boolean satisfiesNumber = !this.number || !pass.replaceAll("[0-9]", "").equals(pass);
				
				if (satisfiesLowercase && satisfiesUppercase && satisfiesNumber) {
					if (this.uppercase) {
						for (int j=0; j<this.length - 1; j++) {
							char c = pass.charAt(j);
							if (Character.isLetter(c)) {
								pass = pass.substring(0, j).concat(String.valueOf(Character.toUpperCase(c))).concat(pass.substring(j + 1));
								break;
							}
						}
					}
					if (this.symbol) {
						char symbol = Service.symbols[Integer.parseInt(shaSum.substring(i + this.length - 1, i + this.length), 16) % Service.symbols.length];
						pass = pass.concat(String.valueOf(symbol));
					} else {
						pass = pass.concat(String.valueOf(shaSum.charAt(i + this.length - 1)));
					}
					
					return pass;
				}
			}
			
			return null;
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	
	public String pack() {
		return String.format("%s;%d;%b;%b;%b;%b", this.name, this.length, this.lowercase, this.uppercase, this.number, this.symbol);
	}
	
	public boolean equals(Service other) {
		return this.pack().equals(other.pack());
	}
	
	public Service(String packed) {
		String[] values = packed.split(";");
		this.name = String.valueOf(values[0]);
		this.length = Integer.parseInt(values[1]);
		this.lowercase = Boolean.parseBoolean(values[2]);
		this.uppercase = Boolean.parseBoolean(values[3]);
		this.number = Boolean.parseBoolean(values[4]);
		this.symbol = Boolean.parseBoolean(values[5]);
	}

	public Service() {
	}

	public void copyVals(Service other) {
		this.name = other.name;
		this.length = other.length;
		this.lowercase = other.lowercase;
		this.uppercase = other.uppercase;
		this.number = other.number;
		this.symbol = other.symbol;
	}
}
