package leonid.flow.workflow;

import org.junit.Assert;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import org.junit.Test;

public class WorkflowTest
{
  @Test(expected=FlowBuildException.class)
  public void testHangingFlowActivity()
  {
    System.out.println("****************************************** testHangingFlowActivity **************************************************");
    
    Flow<String>  flow;
    
    flow = Flow.create("testHangingFlowActivity");

    flow.fromStart().to("First");
    flow.from("First").to("Two");
    flow.from("No two").to("Three");
    flow.from("Three").toEnd(); 
        
    flow.build();    
  }
  
  @Test(expected=FlowBuildException.class)
  public void testHangingAfterIfFlowActivity()
  {
    System.out.println("****************************************** testHangingAfterIfFlowActivity **************************************************");
    
    Flow<String>  flow;
    
    flow = Flow.create("testHangingAfterIfFlowActivity");

    flow.fromStart().to("First");
    
    flow.from("First").
      onlyIf("Need two").to("Two").
      otherwise("Three");
    
    flow.from("No two").to("Three");
    flow.from("Three").toEnd(); 
        
    flow.build();    
  }
  
  @Test(expected=FlowBuildException.class)
  public void testMissedStartActivity()
  {
    System.out.println("****************************************** testOtherwiseToEnd **************************************************");
    
    Flow<String>  flow;
    
    flow = Flow.create("testMissedStartActivity");

    flow.from("First").
      onlyIf("Two").to("Two").
      otherwise().toEnd();
    
    flow.from("Two").to("Three");
    flow.from("Three").toEnd();  
        
    flow.build();    
  }
  
  @Test(expected=FlowBuildException.class)
  public void testOtherwiseToEnd()
  {
    System.out.println("****************************************** testOtherwiseToEnd **************************************************");
    
    Flow<String>  flow;
    
    flow = Flow.create("testOtherwiseToEnd");

    flow.fromStart().to("First");
    
    flow.from("First").
      onlyIf("Two").to("Two").
      otherwise().to("Three");
    
    flow.from("Two").to("Three");
    flow.from("Three").toEnd();  
        
    flow.build();    
  }
  
  @Test(expected=FlowBuildException.class)
  public void testMissedOtherwise()
  {
    System.out.println("****************************************** testMissedOtherwise **************************************************");
    
    Flow<String>  flow;
    
    flow = Flow.create("testMissedOtherwise");

    flow.fromStart().to("First");
    
    flow.from("First").onlyIf("Two").to("Two");
   
    flow.from("Two").to("Three");
    flow.from("Three").toEnd();  
        
    flow.build();    
  }
  
  @Test(expected=FlowBuildException.class)
  public void testMissedOtherwiseAfterElseIf()
  {
    System.out.println("****************************************** testMissedOtherwiseAfterElseIf **************************************************");
    
    Flow<String>  flow;
    
    flow = Flow.create("testMissedOtherwiseAfterElseIf");

    flow.fromStart().to("First");
    
    flow.from("First").
      onlyIf("Two").to("Two").
      elseIf("Three").to("Three");
   
    flow.from("Two").to("Three");
    flow.from("Three").toEnd();  
        
    flow.build();    
  }
  
  @Test(expected=FlowBuildException.class)
  public void testMissedIf()
  {
    System.out.println("****************************************** testMissedIf **************************************************");
    
    Flow<String>  flow;
    
    flow = Flow.create("testMissedIf");

    flow.fromStart().to("First");
    
    flow.from("First").
      elseIf("Three").to("Three").
      otherwise("Four");
   
    flow.from("Two").to("Three");
    flow.from("Three").to("Four").toEnd();  
        
    flow.build();    
  }
  
  @Test(expected=FlowBuildException.class)
  public void testMissedEnd()
  {
    System.out.println("****************************************** testMissedEnd **************************************************");
    
    Flow<String>  flow;
    
    flow = Flow.create("testMissedEnd");

    flow.fromStart().to("First");
    
    flow.from("First").
      onlyIf("Two").to("Two").
      elseIf("Three").to("Three").
      otherwise("Four");
   
    flow.from("Two").to("Three");
    flow.from("Three").to("Four");  
    flow.from("Four").to("Two");
        
    flow.build();    
  }
  
  @Test
  public void testFlowAndConditions()
  {
    System.out.println("****************************************** testFlowAndConditions **************************************************");
    
    Flow<Integer>  flow;
    
    flow = Flow.create("testFlowAndConditions");

    flow.fromStart().to(1);
    flow.from(1).onlyIf(1).to(2).otherwise(4);
    flow.from(2).to(3).to(4).toEnd(); 
        
    flow.build();    
    
    System.out.println("------------BUILT FLOW------------------");
    System.out.println(flow.toString());
    System.out.println("----------------------------------------");
    
    Map<Integer,Boolean> conditions = new HashMap<>();
    List<Integer>        expectedChain; 
    
    conditions.put(1, true);
    
    expectedChain = Arrays.asList(1,2,3,4);
    
    Operation<Integer>    operation = new Operation<>(expectedChain, conditions);
    
    FlowInstance<Integer> flowInstance = flow.getFlowInstance(operation);
    
    runManualFlow(flowInstance);
  }
  
  @Test
  public void testElseIfConditions()
  {
    System.out.println("****************************************** testElseIfConditions **************************************************");
    
    Flow<Integer>  flow;
    
    flow = Flow.create("testElseIfConditions");

    flow.fromStart().to(1);
    flow.from(1).
      onlyIf(100).to(2).
      elseIf(200).to(3).
      elseIf(300).to(4).
      elseIf(400).toEnd().
      otherwise(5);
    flow.from(2).to(3).to(4).to(5).toEnd(); 
        
    flow.build();    
    
    System.out.println("------------BUILT FLOW------------------");
    System.out.println(flow.toString());
    System.out.println("----------------------------------------");
    
    Map<Integer,Boolean> conditions = new HashMap<>();
    List<Integer>        expectedChain = new ArrayList<>(); 
    
    conditions.put(100, true);
    conditions.put(200, true);
    conditions.put(300, true);
    conditions.put(400, false);
    
    Collections.addAll(expectedChain, 1,2,3,4,5);
    
    Operation<Integer>    operation = new Operation<>(expectedChain, conditions);
    FlowInstance<Integer> flowInstance = flow.getFlowInstance(operation);
    
    runManualFlow(flowInstance);
    
    conditions.put(100, false);
    expectedChain.clear();
    Collections.addAll(expectedChain, 1,3,4,5);
    flowInstance.reset();
    runManualFlow(flowInstance);
    
    conditions.put(200, false);
    expectedChain.clear();
    Collections.addAll(expectedChain, 1,4,5);
    flowInstance.reset();
    runManualFlow(flowInstance);
    
    conditions.put(300, false);
    expectedChain.clear();
    Collections.addAll(expectedChain, 1,5);
    flowInstance.reset();
    runManualFlow(flowInstance);
    
    conditions.put(400, true);
    expectedChain.clear();
    Collections.addAll(expectedChain, 1);
    flowInstance.reset();
    runManualFlow(flowInstance);
  }
  
  @Test
  public void testNestedConditions()
  {
    System.out.println("****************************************** testNestedConditions **************************************************");    
    Flow<Integer>  flow;
    
    flow = Flow.create("testNestedConditions");

    flow.fromStart().to(1);
    flow.from(1).
      onlyIf(100).
        onlyIf(200).
          to(2).
        elseIf(300).
          onlyIf(3000).
            to(3).
          otherwise().toEnd().
        otherwise(4).
      elseIf(400).
        to(5).
      otherwise().toEnd();
    flow.from(2).to(3).to(4).to(5).toEnd(); 
        
    flow.build();    
    
    System.out.println("------------BUILT FLOW------------------");
    System.out.println(flow.toString());
    System.out.println("----------------------------------------");
    
    Map<Integer,Boolean> conditions = new HashMap<>();
    List<Integer>        expectedChain = new ArrayList<>(); 
    
    conditions.put(100, true);
    conditions.put(200, true);
    conditions.put(300, false);
    conditions.put(400, true);
    conditions.put(3000, true);
        
    Collections.addAll(expectedChain, 1,2,3,4,5);
    
    Operation<Integer>    operation = new Operation<>(expectedChain, conditions);
    FlowInstance<Integer> flowInstance = flow.getFlowInstance(operation);
    int                   roundNum = 1;
    
    System.out.println ("Round #" + (roundNum++));
    runManualFlow(flowInstance);
        
    conditions.put(200, false);
    expectedChain.clear();
    Collections.addAll(expectedChain, 1,4,5);
    flowInstance.reset();
    
    System.out.println ("Round #" + (roundNum++));
    runManualFlow(flowInstance);
    
    conditions.put(100, false);
    expectedChain.clear();
    Collections.addAll(expectedChain, 1,5);
    flowInstance.reset();
    
    System.out.println ("Round #" + (roundNum++));    
    runManualFlow(flowInstance);
    
    conditions.put(400, false);
    expectedChain.clear();
    Collections.addAll(expectedChain, 1);
    flowInstance.reset();
    
    System.out.println ("Round #" + (roundNum++));
    runManualFlow(flowInstance);
    
    conditions.put(300, true);
    expectedChain.clear();
    Collections.addAll(expectedChain, 1);
    flowInstance.reset();
    
    System.out.println ("Round #" + (roundNum++));
    runManualFlow(flowInstance);
    
    conditions.put(100, true);
    expectedChain.clear();
    Collections.addAll(expectedChain, 1, 3, 4, 5);
    flowInstance.reset();
    
    System.out.println ("Round #" + (roundNum++));
    runManualFlow(flowInstance);
  }
  
  @Test
  public void testResetTo()
  {
    System.out.println("****************************************** testResetTo **************************************************");    
    Flow<Integer>  flow;
    
    flow = Flow.create("testResetTo");

    flow.fromStart().to(1);
    flow.from(1).
      onlyIf(100).
        onlyIf(200).
          to(2).
        elseIf(300).
          onlyIf(3000).
            to(3).
          otherwise().toEnd().
        otherwise(4).
      elseIf(400).
        to(5).
      otherwise().toEnd();
    flow.from(2).to(3).to(4).to(5).toEnd(); 
        
    flow.build();    
    
    System.out.println("------------BUILT FLOW------------------");
    System.out.println(flow.toString());
    System.out.println("----------------------------------------");
    
    Map<Integer,Boolean> conditions = new HashMap<>();
    List<Integer>        expectedChain = new ArrayList<>(); 
    
    conditions.put(100, true);
    conditions.put(200, true);
    conditions.put(300, false);
    conditions.put(400, true);
    conditions.put(3000, true);
        
    Collections.addAll(expectedChain, 3,4,5);
    
    Operation<Integer>    operation = new Operation<>(expectedChain, conditions);
    FlowInstance<Integer> flowInstance = flow.getFlowInstance(operation);
    
    flowInstance.resetTo(3);
    
    runManualFlow(flowInstance);
  }
  
  protected void runManualFlow(FlowInstance<?> flowInstance)
  {
    for (int i=0; !flowInstance.isFinished(); i++)
    {
      System.out.println ("Iteration #" + i);
      flowInstance.move();
    }
  }

  protected class Operation<T> implements FlowOperation<T>
  {
    List<T>         expectedChain;
    Map<T,Boolean>  conditions;
    int             curIndex = 0;
    
    Operation (List<T> expectedChain, Map<T,Boolean> conditions)
    {
      this.expectedChain = expectedChain;
      this.conditions = conditions;
    }
    
    @Override
    public void fromStart(T to)
    {
      System.out.println("---> from start to=" + to);
      
      Assert.assertNotNull("[fromStart] Received NULL to activity", to);
      
      int   size = expectedChain.size();
      
      Assert.assertTrue("[changed] Expected chain does not contain any activity", size > 0);
      
      T     expectedNode = expectedChain.get(0);
      
      Assert.assertNotNull("[fromStart] Could not find any activity right after flow start", expectedNode); 
      Assert.assertEquals("[fromStart] Could not find expected activity right after flow start", expectedNode, to);
      
      curIndex = 0;
    }
    
    @Override
    public void changed(T from, T to)
    {
      System.out.println("---> changed from=" + from + "; to=" + to);
      
      Assert.assertNotNull("[changed] Received NULL from activity", from);
      Assert.assertNotNull("[changed] Received NULL to activity", to);
      
      int   size = expectedChain.size();
      int   fromIndex = curIndex;
      int   toIndex = ++curIndex;
      
      Assert.assertTrue("[changed] Expected chain is ended before getting actual from activity. fromIndex=" + fromIndex, fromIndex < size);
      Assert.assertTrue("[changed] Expected chain is ended before getting actual to activity. toIndex=" + toIndex, toIndex < size);
      
      T   expectedFromNode = expectedChain.get(fromIndex); 
      T   expectedToNode = expectedChain.get(toIndex);
      
      Assert.assertNotNull("[changed] Could not find any activity on index=" + fromIndex, from);
      Assert.assertNotNull("[changed] Could not find any activity on index=" + toIndex, to);
      
      Assert.assertEquals("[changed] Could not find expected activity in place of from activity", expectedFromNode, from);
      Assert.assertEquals("[changed] Could not find expected activity in place of to activity", expectedToNode, to);
    }
    
    @Override
    public void toEnd(T from)
    {
      System.out.println("---> to end from=" + from);
      
      Assert.assertNotNull("[toEnd] Received NULL from activity", from);
      
      T   expectedNode = expectedChain.get(expectedChain.size()-1); 
      
      Assert.assertNotNull("[toEnd] Could not find any activity right before flow end. Index=" + (expectedChain.size()-1), expectedNode);
      Assert.assertEquals("[toEnd] Could not find expected activity right before flow end", expectedNode, from);
    }

    @Override
    public boolean test(T condition)
    {
      Boolean conditionValue = conditions.get(condition);
      
      System.out.println("------> condition[" + condition + "]=" + conditionValue);
      
      Assert.assertNotNull("[test] Received NULL condition", condition);
      Assert.assertNotNull("[test] Received NULL condition simulation value for condition=" + condition, conditionValue);
      
      return conditionValue;
    }
  }
}
