package cn.syncdemo.java;

import javax.swing.JPanel;

public class Ticket{
	//ticket动画相关
	private int station;	//ticket所在出售窗口
	public int x;			//ticket X坐标
	public int y;			//ticket Y坐标
	
	public Ticket(int staion){
		this.station = station;
	}
	
	public Ticket(){
		super();
	}
	
	public int getStation() {
		return station;
	}

	public void setStation(int station) {
		this.station = station;
	}

}
