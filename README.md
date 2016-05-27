# Simple Workflow
This is the simple utility to maintain the object state by specifying the activity flow.
Java Doc can be viewed here: http://lbazhenov.github.io/workflow/apidocs/

## Example
<pre>
 // Create a flow builder, where every node name is represented by String value
 Flow&lt;String&gt;  flow = Flow.create("MyFlow");
 
 // Code the flow
 flow.fromStart().to("FirstState");
 flow.from("FirstState").
   onlyIf("Condition-1").
     onlyIf("Condition-2).
       to("SecondState").
     elseIf("Condition-3").
       onlyIf("Condition-4").
         to("ThirdState").
       otherwise().toEnd().
     otherwise("FourthState").
   elseIf("Condition-5").
     to("FifthState").
   otherwise().toEnd();
   
  flow.build(); // Now the flow of nodes is coded. 
  
  // The next we need:
  // [1] to create the map of NodeName->Operation to bring the actual execution for each node.
  // [2] to request flow instance for such operational map.

  // Map of operations
  Map&lt;String,Operation&lt;String&gt;&gt; operations = new HashMap&lt;&gt;();
  
  // Default operation if some of the node is not mapped to operation. It's optional
  Operation&lt;String&gt;              defaultOperation = new MyDefaultOperation();  
 
  // Fill "operations" map here...
  // Something like...
  // operations.put("FirstState", new YourFirstStateImpl());
  // Note: you may reuse the same state implementation but in this case it will require 
  // conditional logic based on node name.
  
  // Request the instance of the flow
  FlowInstance&lt;String&gt;           flowInstance = flow.getFlowInstance(operations, defaultOperation);   
 
 // Do your business with invoking move() to make a transition...
 flowInstance.move(); // Operation from "operations" map or default one is used to make a call back 
 
 // .....
</pre>
