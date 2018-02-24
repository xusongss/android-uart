package com.inspiry.barcodeupdate.build;
import com.inspiry.barcodeupdate.BarCodeSerialUpdateWrapper;
public class util {
	public static void main(String args[]) {
		if (args.length < 1) {
			System.out.println("error: param");
			return;
        }
		if(args[0].equals("getVersion"))
		{
			System.out.println(BarCodeSerialUpdateWrapper.getApiVersion());
			return;
		}
		System.out.println("error: param");
         
    } 

}
