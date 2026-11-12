package androidmanager.jni;

#if android
import lime.system.JNI;
#end

class JNICache {
	#if android
	private static var cache:Map<String, Dynamic> = new Map();
	#end

	public static function getStaticMethod(className:String, methodName:String, signature:String, useCache:Bool = true):Dynamic {
		#if android
		var key:String = className + "::" + methodName + "::" + signature;

		if (useCache && cache.exists(key))
			return cache.get(key);

		var method:Dynamic = null;
		try {
			method = JNI.createStaticMethod(className, methodName, signature, false, true);
		} catch (e:Dynamic) {
			method = null;
		}

		if (method != null && useCache)
			cache.set(key, method);

		return method;
		#else
		return null;
		#end
	}

	public static function getMemberMethod(className:String, methodName:String, signature:String, useCache:Bool = true):Dynamic {
		#if android
		var key:String = className + "::MEMBER::" + methodName + "::" + signature;

		if (useCache && cache.exists(key))
			return cache.get(key);

		var method:Dynamic = null;
		try {
			method = JNI.createMemberMethod(className, methodName, signature, false, true);
		} catch (e:Dynamic) {
			method = null;
		}

		if (method != null && useCache)
			cache.set(key, method);

		return method;
		#else
		return null;
		#end
	}

	public static function getStaticField(className:String, fieldName:String, signature:String, useCache:Bool = true):Dynamic {
		#if android
		var key:String = className + "::FIELD::" + fieldName + "::" + signature;

		if (useCache && cache.exists(key))
			return cache.get(key);

		var field:Dynamic = null;
		try {
			field = JNI.createStaticField(className, fieldName, signature);
		} catch (e:Dynamic) {
			field = null;
		}

		if (field != null && useCache)
			cache.set(key, field);

		return field;
		#else
		return null;
		#end
	}

	public static function call(className:String, methodName:String, signature:String, ?args:Array<Dynamic>, ?fallback:Dynamic):Dynamic {
		#if android
		var method:Dynamic = getStaticMethod(className, methodName, signature);
		if (method == null)
			return fallback;

		try {
			return Reflect.callMethod(null, method, args != null ? args : []);
		} catch (e:Dynamic) {
			return fallback;
		}
		#else
		return fallback;
		#end
	}

	public static function clear():Void {
		#if android
		cache.clear();
		#end
	}
}
