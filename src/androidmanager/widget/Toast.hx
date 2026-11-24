package androidmanager.widget;

import androidmanager.content.Interface;

class Toast {
	public static inline final LENGTH_SHORT:Int = 0;
	public static inline final LENGTH_LONG:Int = 1;

	public static function makeText(text:String, duration:Int = LENGTH_SHORT, gravity:Int = -1, xOffset:Int = 0, yOffset:Int = 0):Void
		Interface.makeToast(text, duration == LENGTH_LONG ? LONG : SHORT, gravity, xOffset, yOffset);
}
