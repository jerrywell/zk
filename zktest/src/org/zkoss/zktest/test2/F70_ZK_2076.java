package org.zkoss.zktest.test2;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventQueues;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zkmax.ui.select.annotation.Subscribe;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;

public class F70_ZK_2076 extends SelectorComposer<Window> {
	
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Wire
    Textbox t1;
    @Wire
    Textbox t2;
    @Wire
    Textbox t3;
     
    @Listen("onClick=#b1")
    public void b1() {
    	EventQueues.lookup("test", EventQueues.DESKTOP, true).publish(new Event("b1"));
    }
    
    @Listen("onClick=#b2")
    public void b2() {
    	EventQueues.lookup("test", EventQueues.DESKTOP, true).publish(new Event("b2"));
    }
    
    @Listen("onClick=#b3")
    public void b3() {
    	EventQueues.lookup("test", EventQueues.DESKTOP, true).publish(new Event("b3"));
    }
    
    @Listen("onClick=#clean")
    public void clean() {
        t1.setValue("");
        t2.setValue("");
        t3.setValue("");
    }
    
    @Subscribe(value = "test")
    public void b1Test() {
    	t1.setValue("test");
    }
    
    @Subscribe(value = "test", eventName="b2")
    public void b2Test() {
    	t2.setValue("test");
    }
    
    @Subscribe(value = "test", eventName="b4")
    public void b3Test() {
    	t3.setValue("test");
    }
}
