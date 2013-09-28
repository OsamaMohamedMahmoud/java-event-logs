package jvm.pm.tool.logs;

import java.util.Calendar;

public class Entry {
	private String title;

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getOrig() {
		return orig;
	}

	public void setOrig(String orig) {
		this.orig = orig;
	}

	public Calendar getTime() {
		return time;
	}

	public void setTime(Calendar time) {
		this.time = time;
	}

	public Object getObj() {
		return obj;
	}

	public void setObj(Object obj) {
		this.obj = obj;
	}

	private String status;
	private String orig;
	private Calendar time;
	private Object obj;

	public Entry(String title, String status, String orig, Calendar time, Object obj) {
		this.obj = obj;
		this.time = time;
		this.title = title;
		this.orig = orig;
		this.status = status;
	}
}