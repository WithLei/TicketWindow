package cn.syncdemo.java;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JLabel;
import java.awt.Font;

import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.awt.event.ActionEvent;
import javax.swing.ImageIcon;

import java.awt.Color;

public class MainActivity{
	private static MainActivity window;
	private JFrame frame;
	private JButton btn_start;
	private JButton btn_stop;
	private JLabel totalLabel;
	private JLabel sellLabel;
	private JLabel totalNumLabel;
	private JLabel sellFirstLabel;
	private JLabel sellSecondLabel;
	private JLabel sellThirdLabel;
	
	//运行三种状态 1-准备  2-进行中  3-暂停  4-结束
	private static final int STATE_ALREADY = 1;
	private static final int STATE_RUNNING = 2;
	private static final int STATE_PAUSE = 3;
	private static final int STATE_END = 4;
	
	//售票台
	private static final int FirstStation = 1;
	private static final int SecondStation = 2;
	private static final int ThirdStation = 3;
	
	//每个售票台售出票数
	private int FirstSold = 0;
	private int SecondSold = 0;
	private int ThirdSold = 0;
	
	//窗口售票状态 1-准备  2-进行中  3-暂停  4-结束
	private int window_state = STATE_ALREADY;
	
	//三个售票窗口线程
	private Thread t1;
	private Thread t2;
	private Thread t3;
	
	//车票总数
	private static final int TOTAL_TICKET = 100;
	//初始化车票池
	private List<Ticket>tickets = initData();
	//初始化车票图标
	private JLabel[] ticketLabel = initLabel();
	//初始化车票下落动画线程
	private Thread[] ticketThread = initTicket();

	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					//初始化界面
					window = new MainActivity();
					window.frame.setVisible(true);
					
					//对按钮等设置监听
					window.initListener();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	//窗口线程run方法实现
	class TicketWindowThread implements Runnable{
		private int ticketNum(){
			return tickets.size();
		}
		
		@Override
		public void run() {
			if(ticketNum() <= 0)
				stopSale();
			while(ticketNum() > 0){
				//线程终止
				if(Thread.interrupted())
					return;
				
				//对售票过程使用同步锁
				synchronized (this) {
					if(ticketNum() > 0){
						//判断哪个线程【售票窗口】在对车票进行操作
						String stationName = Thread.currentThread().getName();
						int station;
						if(stationName.equals("1号售票窗口")){
							station = 1;
							FirstSold++;
						}
						else if(stationName.equals("2号售票窗口")){
							station = 2;
							SecondSold++;
						}
						else{
							station = 3;
							ThirdSold++;
						}
						tickets.get(ticketNum()-1).setStation(station);
						
						//播放车票移动动画
						move(ticketNum()-1);
						try {
							//每个售票窗口每隔1000ms售出一张票
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						tickets.remove(tickets.size()-1);
					}
				}
			}
				
		}
	}

	public void initListener() {
		//对售票按钮进行监听
		btn_start.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				switch(window_state){
					case STATE_ALREADY:
						//点击开始/重新开始售票
						startSale();
						break;
					case STATE_RUNNING:
						//点击暂停售票
						pauseSale();
						break;
					case STATE_PAUSE:
						//点击继续售票
						continueSale();
						break;
					case STATE_END:
						//点击停止售票
						stopSale();
						break;
				}
			}
		});
		
		//对停止按钮进行监听
		btn_stop.addActionListener(new ActionListener() {
					
			@Override
			public void actionPerformed(ActionEvent e) {
				//点击停止售票
				stopSale();
			}
		});
	}
	
	//开始售票
		public void startSale(){
			//初始化车票池
			tickets = initData();
			//初始化车票图标
			ticketLabel = initLabel();
			//初始化车票下落动画线程
			ticketThread = initTicket();
			//初始化窗口出票数
			FirstSold = 0;
			SecondSold = 0;
			ThirdSold = 0;
			//初始化窗口线程
			TicketWindowThread twThread = new TicketWindowThread();
			
			t1 = new Thread(twThread,"1号售票窗口");
			t2 = new Thread(twThread,"2号售票窗口");
			t3 = new Thread(twThread,"3号售票窗口");
			 
			t1.start();
			t2.start();
			t3.start();
			 
			//置按钮为【暂停】
			btn_start.setText("暂停售票");
			btn_stop.setEnabled(true);
			window_state = STATE_RUNNING;
			//隐藏售票数frame
			hideOverFrame();
		}
		
		//暂停售票
		@SuppressWarnings("deprecation")
		public void pauseSale(){
			try{
				//暂停售票线程
				t1.suspend();
				t2.suspend();
				t3.suspend();
				
				//暂停ticket线程
				for(int i=0;i<TOTAL_TICKET;i++)
					if(ticketThread[i] != null){
						ticketThread[i].suspend();
					}
						
			}catch(Exception e){
				e.printStackTrace();
			}finally{
				//置按钮为【继续】
				btn_start.setText("继续售票");
				window_state = STATE_PAUSE;
			}
			
		}
		
		//点击继续售票
		@SuppressWarnings("deprecation")
		public void continueSale(){
			//恢复售票线程
			t1.resume();
			t2.resume();
			t3.resume();
			
			//恢复ticket线程
			for(int i=0;i<TOTAL_TICKET;i++)
				if(ticketThread[i] != null)
					ticketThread[i].resume();
			
			//置按钮为【暂停】
			btn_start.setText("暂停售票");
			window_state = STATE_RUNNING;
		}
		
		//点击结束售票
		public void stopSale(){
			//结束前调用pauseSale()暂停线程再结束
			pauseSale();
			
			//结束售票进程
			t1.interrupt();
			t2.interrupt();
			t3.interrupt();
			
			//结束ticket线程
			for(int i=0;i<TOTAL_TICKET;i++){
				if(ticketThread[i] != null)
					ticketThread[i].interrupt();
				//删除所有的ticketLabel
				ticketLabel[i].setVisible(false);
				ticketLabel[i] = null;
			}
			
			btn_start.setText("重新开始售票");
			btn_stop.setEnabled(false);
			window_state = STATE_ALREADY;
			//显示售票数frame
			showOverFrame();
		}
	
	//车票移动动画效果
	public void move(final int index){
		//实现车票下落动画线程
		ticketThread[index] = new Thread(){
			
			@Override
			public void run() {
				//线程终止
				if(Thread.interrupted()){
					return;
				}
				
				final Ticket ticket = tickets.get(index);
					
				//判断是哪个车站出票决定票X轴位置
				int pointX = 0;
				switch(ticket.getStation()){
					case FirstStation:
						pointX = 100;
						break;
					case SecondStation:
						pointX = 400;
						break;
					case ThirdStation:
						pointX = 700;
						break;		
				};
				final int X = pointX;
				ImageIcon ticketImg = null;
					
				//随机出红蓝车票
				Random rand = new Random();
				if(rand.nextBoolean())
					ticketImg = new ImageIcon(getClass().getResource("ticket1.png"));
				else
					ticketImg = new ImageIcon(getClass().getResource("ticket2.png"));
					
				//初始化车票Label对象
				ticketLabel[index].setIcon(ticketImg);
				ticketLabel[index].setBounds(pointX,100,200,200);
					
				//竖直方向移动车票Label
				tickets.get(index).x = pointX;
				tickets.get(index).y = 200;
				frame.add(ticketLabel[index],0);
				new Thread(){

					@Override
					public void run() {
						synchronized (this) {
							for(;ticket.y < 600;ticket.y += 5){
								//更新ticket位置
								if(ticketLabel[index] != null)
									ticketLabel[index].setBounds(X, ticket.y, 64, 40);
								else 
									return;
								//更新位置间隔时间100ms
								try {
									Thread.sleep(100);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
								
							//到达后设置不可见
							ticketLabel[index].setVisible(false);
							frame.remove(ticketLabel[index]);
							if(tickets.size() == 0)
								stopSale();
						}
						
					}
					
				}.start();
			}
		};
		
		//开启车票线程
		ticketThread[index].start();
	}
	
	//初始化车票池
	public ArrayList<Ticket> initData(){
		ArrayList<Ticket>tickets = new ArrayList<Ticket>();
		for(int i=0;i<TOTAL_TICKET;i++)
			tickets.add(new Ticket());
		return tickets;
	}
	
	//初始化车票Label
	public JLabel[] initLabel(){
		JLabel[] labels = new JLabel[TOTAL_TICKET+5];
		for(int i=0;i<TOTAL_TICKET;i++)
			labels[i] = new JLabel();
		return labels;
	}
	
	//初始化车票下落动画线程
	public Thread[] initTicket(){
		Thread[]ticketThread = new Thread[TOTAL_TICKET+5];
		return ticketThread;
	}
	
	public MainActivity() {
		initialize();
	}

	//布局
	private void initialize() {
		frame = new JFrame();
		frame.setSize(960, 800);
		frame.setDefaultCloseOperation(3);
		frame.setLocationRelativeTo(null);
		frame.getContentPane().setBackground(Color.WHITE);
		frame.setForeground(Color.WHITE);
		frame.setFont(new Font("微软雅黑", Font.PLAIN, 12));
		frame.setTitle("模拟窗口售票流程界面");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		
		JLabel bg = new JLabel();
		ImageIcon background = new ImageIcon(getClass().getResource("background.png"));
		bg.setBounds(0, 0, 960, 800);
		bg.setIcon(background);
		frame.add(bg);
		
		JLabel title1 = new JLabel("一号窗口");
		title1.setBounds(140,30,200,50);
		title1.setFont(new Font("幼圆", Font.PLAIN, 18));
		frame.add(title1,0);

		JLabel title2 = new JLabel("二号窗口");
		title2.setBounds(440,30,200,50);
		title2.setFont(new Font("幼圆", Font.PLAIN, 18));
		frame.add(title2,0);
		
		JLabel title3 = new JLabel("三号窗口");
		title3.setBounds(740,30,200,50);
		title3.setFont(new Font("幼圆", Font.PLAIN, 18));
		frame.add(title3,0);
		
		ImageIcon stationImg = new ImageIcon(getClass().getResource("station.png"));
		JLabel firstStation = new JLabel();
		firstStation.setIcon(stationImg);
		firstStation.setBounds(100,60,200,256);
		frame.add(firstStation,0);
		
		JLabel secondStation = new JLabel();
		secondStation.setIcon(stationImg);
		secondStation.setBounds(400,60,200,256);
		frame.add(secondStation,0);
		
		JLabel thirdStation = new JLabel();
		thirdStation.setIcon(stationImg);
		thirdStation.setBounds(700,60,200,256);
		frame.add(thirdStation,0);
		
		btn_start = new JButton("开始售票");
		btn_start.setFont(new Font("微软雅黑", Font.PLAIN, 15));
		btn_start.setBounds(250,700,140,30);
		frame.add(btn_start,0);
		
		btn_stop = new JButton("停止售票");
		btn_stop.setFont(new Font("微软雅黑", Font.PLAIN, 15));
		btn_stop.setBounds(550,700,140,30);
		btn_stop.setEnabled(false);
		frame.add(btn_stop,0);
		
		totalLabel = new JLabel();
		ImageIcon totalImg = new ImageIcon(getClass().getResource("total.png"));
		totalLabel.setIcon(totalImg);
		totalLabel.setBounds(200,300,380,61);
		frame.add(totalLabel,0);
		
		sellLabel = new JLabel();
		ImageIcon sellImg = new ImageIcon(getClass().getResource("sell.png"));
		sellLabel.setIcon(sellImg);
		sellLabel.setBounds(350,400,400,200);
		frame.add(sellLabel,0);
		
		totalNumLabel = new JLabel();
		totalNumLabel.setFont(new Font("幼圆",Font.BOLD,40));
		totalNumLabel.setForeground(Color.white);
		totalNumLabel.setBounds(450,290,100,100);
		frame.add(totalNumLabel,0);
		
		sellFirstLabel = new JLabel();
		sellFirstLabel.setText("57");
		sellFirstLabel.setFont(new Font("幼圆",Font.BOLD,40));
		sellFirstLabel.setForeground(Color.white);
		sellFirstLabel.setBounds(632,380,100,100);
		frame.add(sellFirstLabel,0);
		
		sellSecondLabel = new JLabel();
		sellSecondLabel.setFont(new Font("幼圆",Font.BOLD,40));
		sellSecondLabel.setForeground(Color.white);
		sellSecondLabel.setBounds(632,455,100,100);
		frame.add(sellSecondLabel,0);
		
		sellThirdLabel = new JLabel();
		sellThirdLabel.setFont(new Font("幼圆",Font.BOLD,40));
		sellThirdLabel.setForeground(Color.white);
		sellThirdLabel.setBounds(632,525,100,100);
		frame.add(sellThirdLabel,0);
		
		hideOverFrame();
	}
	
	private void showOverFrame(){
		totalLabel.setVisible(true);
		sellLabel.setVisible(true);
		int total = FirstSold + SecondSold + ThirdSold;
		totalNumLabel.setText(String.valueOf(total));
		totalNumLabel.setVisible(true);
		sellFirstLabel.setText(String.valueOf(FirstSold));
		sellFirstLabel.setVisible(true);
		sellSecondLabel.setText(String.valueOf(SecondSold));
		sellSecondLabel.setVisible(true);
		sellThirdLabel.setText(String.valueOf(ThirdSold));
		sellThirdLabel.setVisible(true);
	}
	
	private void hideOverFrame(){
		totalLabel.setVisible(false);
		sellLabel.setVisible(false);
		totalNumLabel.setVisible(false);
		sellFirstLabel.setVisible(false);
		sellSecondLabel.setVisible(false);
		sellThirdLabel.setVisible(false);
	}
}
