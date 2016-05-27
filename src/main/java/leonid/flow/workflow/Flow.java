package leonid.flow.workflow;

import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

/**
 * <p>This is the flow builder, which can be used to create a simple state machine to control the sate transition. The flow
 * consists of activities and conditions. <strong>T</strong> is a type of activity and condition IDs. 
 * Once the flow is built it can return any number of <code>FlowInstance</code>'s to be used anywhere where state
 * transition is required to avoid complicated if-else and switch statements with state storage.  
 * </p>
 * <p>The flow builder expects the certain sequence of build methods to be invoked. If the sequence is violated then
 * <code>FlowBuildException</code> is thrown. The sequence if rather intuitive: <code>create()</code> must be followed
 * by <code>fromStart</code>; <code>fromStart()</code> and <code>from()</code> must be followed by <code>to()</code>
 * or by opening the condition block, that is <code>onlyIf()</code>; <code>onlyIf()</code> ultimately must be followed
 * by <code>otherwise()</code> to close condition block; the condition block can internally have any number of
 * <code>elseIf()</code>'s. 
 * </p>  
 * <p>To get the <code>FlowInstance</code> caller must supply the map of <code>Flow.Operation</code>'s. Each flow activity and
 * condition is mapped to <code>Flow.Operation</code> instance. It's possible to use single <code>Flow.Operation</code> instance
 * for all activities and conditions or link just subset activities and conditions and for those, which are not linked, 
 * it's possible to set default <code>Flow.Operation</code>. 
 * </p>
 * 
 * <strong>Example</strong>
 * <pre>
 * Flow&lt;Integer&gt;  flowBuilder = Flow.create("MyFlow");
 *
 * flowBuilder.fromStart().to(1);
 * flowBuilder.from(1).
 *   onlyIf(100).
 *     onlyIf(200).
 *       to(2).
 *     elseIf(300).
 *       onlyIf(3000).
 *         to(3).
 *       otherwise().toEnd().
 *     otherwise(4).
 *   elseIf(400).
 *     to(5).
 *   otherwise().toEnd();
 * flowBuilder.from(2).to(3).to(4).to(5).toEnd().build(); 
 * 
 * Map&lt;Integer,Operation&lt;Integer&gt;&gt; operations = new HashMap&lt;&gt;();
 * Operation&lt;Integer&gt;                    defaultOperation = new MyDefaultOperation();
 * 
 * &#47;&#47; Fill "operations" map here...
 * 
 * FlowInstance&lt;Integer&gt;           flow = flowBuilder.getFlowInstance(operations, defaultOperation);   
 * 
 * &#47;&#47; Do your business with invoking move() to make a transition...
 * flow.move(); &#47;&#47; Operation instance from "operations" map or default one is called back 
 * 
 * &#47;&#47; .....
 * </pre>
 * 
 * 
 * @author Leonid Bazhenov
 * @version 1.0
 */
public class Flow<T>
{
  /**
   * This enumeration defines the flow building states.
   * <ul>
   * <li><strong>CREATE</strong> &ndash; Creation of the flow is just started after invoking <code>create()</code> method.</li>
   * <li><strong>FROM</strong> &ndash; <code>fromStart()</code> (only after <code>create()</code> method) or <code>from()</code> is invoked.</li>
   * <li><strong>TO</strong> &ndash; <code>to()</code> method is invoked.</li>
   * <li><strong>ONLY_IF</strong> &ndash; <code>onlyIf()</code> method is invoked.</li>
   * <li><strong>ELSE_IF</strong> &ndash; <code>elseIf()</code> method is invoked.</li>
   * <li><strong>OTHERWISE</strong> &ndash; <code>otherwise()</code> method is invoked.</li>
   * <li><strong>OTHERWISE_END</strong> &ndash; <code>otherwise()</code> method without any parameter is invoked and <code>toEnd()</code> invocation is expected.</li>
   * <li><strong>BUILT</strong> &ndash; <code>build()</code> method is invoked. Flow is ready to use. User may invoke <code>getFlowInstance()</code> method to get its instance with custom callback operations</li>
   * </ul> 
   */
  protected enum BuildState
  {
    CREATE, FROM, TO, END, ONLY_IF, ELSE_IF, OTHERWISE, OTHERWISE_END, BUILT;
  }
  
  /**
   * Flow node internal representation.
   */
  protected class FlowNode
  {
    protected T               activityID;
    protected FlowNode        nextActivity = null;
    protected List<FlowNode>  activityPerCondition = null;
    protected List<T>         conditionIDs = null;
    
    protected FlowNode (T activityID)
    {
      this.activityID = activityID;
    }
  } 
  
  // Fields, which define the Flow
  protected String            flowName;                         // Flow name
  protected FlowNode          startActivity = null;
  protected FlowNode          endActivity = null;
  protected Map<T,FlowNode>   nodes = new HashMap<>();
  
  // Fields used only for flow building
  protected Deque<FlowNode>   openConds = new LinkedList<>();   // List of nested conditions, which is not yet closed with "otherwise"
  protected BuildState        buildState;                       // The state of flow building. "FlowInstance" can be created only if buildState=BUILT
  protected boolean           isEndPresent = false;             // true if ending activity is found during flow building
  protected FlowNode          latestFromNode;                   // the latest "from" node, which is not yet linked with destination one
  
  protected Flow (String flowName)
  {
    if (flowName == null || (flowName=flowName.trim()).length() == 0)
    {
      throw new FlowBuildException ("Flow name cannot be null or empty!");
    }
    
    this.flowName = flowName;

    buildState = BuildState.CREATE;
  }
  
  /**
   * Creates the initial empty flow. Only <code>fromStart()</code> method is allowed to call on this stage to start the flow.
   * <p>
   * The followings describes the transitions between internal <code>Flow</code> builder states. 
   * <b>&#8744;</b> stands for logical OR. Subscripts depict API invocation, which make the transition.
   * 
   * </p> 
   * 
   * Instance of <code>Flow</code> builder creation: <em>CREATE<sub>create(FlowName)</sub></em>
   * 
   * <p><em>CREATE<b>&nbsp;&#8594;&nbsp;</b>FROM<sub>fromStart()</sub></em></p>
   * 
   * <em>FROM<b>&nbsp;&#8594;&nbsp;</b>TO<sub>to(Activity)</sub><b>&#8744;</b></em>
   * <p style="margin:0em 0em 1em 4em"><em>ONLY_IF<sub>onlyIf(Condition)</sub><b>&#8744;</b></em></p>
   * <p style="margin:0em 0em 1em 4em"><em>END<sub>toEnd()</sub></em></p>
   * 
   * <em>TO<b>&nbsp;&#8594;&nbsp;</b>BUILT<sub>build()</sub><b>&#8744;</b></em>
   * <p style="margin:0em 0em 1em 4em"><em>FROM<sub>from(Activity)</sub><b>&#8744;</b></em></p>
   * <p style="margin:0em 0em 1em 4em"><em>TO<sub>to(Activity)</sub><b>&#8744;</b></em></p>
   * <p style="margin:0em 0em 1em 4em"><em>ONLY_IF<sub>onlyIf(Condition)</sub><b>&#8744;</b></em></p>
   * <p style="margin:0em 0em 1em 4em"><em>ELSE_IF<sub>elseIf(Condition)</sub><b>&#8744;</b></em></p>
   * <p style="margin:0em 0em 1em 4em"><em>OTHERWISE_END<sub>otherwise()</sub><b>&#8744;</b></em></p>
   * <p style="margin:0em 0em 1em 4em"><em>OTHERWISE<sub>otherwise(Activity)</sub><b>&#8744;</b></em></p>
   * <p style="margin:0em 0em 1em 4em"><em>END<sub>toEnd()</sub></em></p>
   * 
   * <em>ONLY_IF<b>&nbsp;&#8594;&nbsp;</b>TO<sub>to(Activity)</sub><b>&#8744;</b></em>
   * <p style="margin:0em 0em 1em 4em"><em>ONLY_IF<sub>onlyIf(Condition)</sub><b>&#8744;</b></em></p>
   * <p style="margin:0em 0em 1em 4em"><em>END<sub>toEnd()</sub></em></p>
   * 
   * <em>ELSE_IF<b>&nbsp;&#8594;&nbsp;</b>TO<sub>to(Activity)</sub><b>&#8744;</b></em>
   * <p style="margin:0em 0em 1em 4em"><em>ONLY_IF<sub>onlyIf(Condition)</sub><b>&#8744;</b></em></p>
   * <p style="margin:0em 0em 1em 4em"><em>END<sub>toEnd()</sub></em></p>
   * 
   * <em>OTHERWISE<b>&nbsp;&#8594;&nbsp;</b>BUILT<sub>build()</sub><b>&#8744;</b></em>
   * <p style="margin:0em 0em 1em 4em"><em>FROM<sub>from(Activity)</sub><b>&#8744;</b></em></p>
   * <p style="margin:0em 0em 1em 4em"><em>TO<sub>to(Activity)</sub><b>&#8744;</b></em></p>
   * <p style="margin:0em 0em 1em 4em"><em>ONLY_IF<sub>onlyIf(Condition)</sub><b>&#8744;</b></em></p>
   * <p style="margin:0em 0em 1em 4em"><em>ELSE_IF<sub>elseIf(Condition)</sub><b>&#8744;</b></em></p>
   * <p style="margin:0em 0em 1em 4em"><em>OTHERWISE_END<sub>otherwise()</sub><b>&#8744;</b></em></p>
   * <p style="margin:0em 0em 1em 4em"><em>OTHERWISE<sub>otherwise(Activity)</sub><b>&#8744;</b></em></p>
   * <p style="margin:0em 0em 1em 4em"><em>END<sub>toEnd()</sub></em></p>
   * 
   * <p><em>OTHERWISE_END<b>&nbsp;&#8594;&nbsp;</b>BUILT<sub>build()</sub><b>&#8744;</b></em><em>END<sub>toEnd()</sub></em>
   * </p>
   * <em>END<b>&nbsp;&#8594;&nbsp;</b>BUILT<sub>build()</sub><b>&#8744;</b></em>
   * <p style="margin:0em 0em 1em 4em"><em>ELSE_IF<sub>elseIf(Condition)</sub><b>&#8744;</b></em></p>
   * <p style="margin:0em 0em 1em 4em"><em>OTHERWISE_END<sub>otherwise()</sub><b>&#8744;</b></em></p>
   * <p style="margin:0em 0em 1em 4em"><em>OTHERWISE<sub>otherwise(Activity)</sub></em></p>
   * 
   * <em>BUILT: This state is final. To get the <code>FlowInstance</code> object to run the flow the <code>getFlowInstance</code>
   * method must be called of the <code>Flow</code> builder object</em>
   * 
   * 
   * <p>
   * Multiple <code>FlowInstance</code> objects can be created for the same flow chart built by <code>Flow</code> builder. 
   * Each of them will run its own instance of the flow chart.</p>
   * @param <C> data type of the flow node id.
   * 
   * @param flowName is the name of the flow.
   * @return <code>Flow</code> instance to continue the flow building.
   */
  public static <C> Flow<C> create(String flowName)
  {
    return new Flow<C>(flowName);
  }
  
  /**
   * Builds the final flow. This method does basic check for:
   * <ul>
   * <li>all conditions are closed, i.e. each <code>onlyIf()</code> is followed by corresponding <code>otherwise()</code>.</li>
   * <li>presence of END activity, i.e. <code>toEnd()</code> method is invoked at least once.</li>
   * <li>obvious infinite loops, i.e. the presence of unconditional transitions, which flow to visited activity.</li> 
   * </ul>
   * 
   * @return <code>Flow</code> instance for which <code>getFlowInstance</code> method can be successfully invoked. 
   */
  public Flow<T> build()
  {
    if (buildState == BuildState.TO ||
        buildState == BuildState.END ||
        buildState == BuildState.OTHERWISE)
    {
      // Check, whether start node goes to anywhere
      if (startActivity.nextActivity == null) throw new FlowBuildException ("Start activity does not point to any node!");
      
      // Check, whether end node is referred by at least one node
      if (! isEndPresent) throw new FlowBuildException ("There is no activity in the flow, which goes to end!");
      
      // Check, whether all conditions are closed
      if (openConds.size() > 0) throw new FlowBuildException ("Not all conditions were closed! Check after: " + openConds.peekFirst().activityID);

      for (FlowNode flowNode: nodes.values())
      {
        if (flowNode.nextActivity == null)
          throw new FlowBuildException ("Found the activity, which does not have next activity to go to. Problematic ActivityId is: " + flowNode.activityID); 
      }
      
      latestFromNode = null;

      //currentNodes.clear();
      buildState = BuildState.BUILT;
    }
    else
    {
      throw new FlowBuildException ("Wrong method invocation! build() can be called only after either to(), toEnd() or otherwise() method call. Found: " + buildState);
    }
    
    return this;
  }
  
  /**
   * Returns the name of the flow.
   * @return name of the flow.
   */
  public String getName ()
  {
    return this.flowName;
  }
 
  /**
   * Returns the flow instance supplied with single operation for all transitions.  
   * 
   * @param defaultOperation is a single operation for all transitions.
   * @return flow instance.
   */
  public FlowInstance<T> getFlowInstance(FlowOperation<T> defaultOperation)
  {
    if (defaultOperation == null)
      throw new FlowBuildException ("Default operation cannot be null!");
    
    if (buildState != BuildState.BUILT)
      throw new FlowBuildException ("Flow must be built first before requesting flow instance! Current building state is: " + buildState);
    
    return new FlowInstanceImpl (defaultOperation);
  }
  
  /**
   * Returns the flow instance supplied with operation map. Every map entry is an activity or condition ID to 
   * <code>Flow.Operation</code> implementation pair.
   * If some of the activity is not linked to <code>Flow.Operation</code> implementation in the map
   * then no notification is thrown when this activity is reached.
   * If some of the condition is not linked to <code>Flow.Operation</code> implementation in the map 
   * then the such condition is evaluated to <code>false</code>. 
   * 
   * @param operationMap is the map of pairs of activity ID or condition ID to <code>Flow.Operation</code> implementation.
   * @return flow instance.
   */
  public FlowInstance<T> getFlowInstance(Map<T,FlowOperation<T>> operationMap)
  {
    if (operationMap == null)
      throw new FlowBuildException ("Operation map cannot be null!");
    
    if (buildState != BuildState.BUILT)
      throw new FlowBuildException ("Flow must be built first before requesting flow instance! Current building state is: " + buildState);
    
    return new FlowInstanceImpl (operationMap);
  }
  
  /**
   * Returns the flow instance supplied with operation map and default operation. Every map entry is an activity or condition ID to 
   * <code>Flow.Operation</code> implementation pair.
   * If some of the activity is not linked to <code>Flow.Operation</code> implementation in the map
   * then notification goes to default operation.
   * If some of the condition is not linked to <code>Flow.Operation</code> implementation in the map 
   * then the such condition is evaluated by invoking <code>test</code> method of default operation. 
   *  
   * @param operationMap is the map of pairs of activity ID or condition ID to <code>Flow.Operation</code> implementation.
   * @param defaultOperation to be used if there is not linkage between reached activity or condition ID to <code>Flow.Operation</code> implementation. 
   * @return flow instance.
   */
  public FlowInstance<T> getFlowInstance(Map<T,FlowOperation<T>> operationMap, FlowOperation<T> defaultOperation)
  {
    if (operationMap == null)
      throw new FlowBuildException ("Operation map cannot be null!");

    if (defaultOperation == null)
      throw new FlowBuildException ("Default operation cannot be null!");
    
    if (buildState != BuildState.BUILT)
      throw new FlowBuildException ("Flow must be built first before requesting flow instance! Current building state is: " + buildState);
    
    return new FlowInstanceImpl (operationMap, defaultOperation);
  }

  public Flow<T> fromStart()
  {
    if (buildState == BuildState.CREATE)
    {
      startActivity = new FlowNode(null);
      endActivity = new FlowNode(null);
      
      latestFromNode = startActivity;
    }
    else
    {
      throw new FlowBuildException ("Wrong method invocation! fromStart() can be called only after creation of Flow instance. Found: " + buildState);
    }
    
    // Set the last method invoked as from(), because fromStart() has the same effect
    buildState = BuildState.FROM;
    
    return this;
  }

  public Flow<T> from(T activityId)
  {
    if (activityId == null) throw new FlowBuildException ("ActivityId cannot be null in \"from\"!");
   
    if (buildState == BuildState.TO ||
        buildState == BuildState.END ||
        buildState == BuildState.OTHERWISE)
    {
      FlowNode    fromNode = nodes.get(activityId); 
      
      // "From" activity should not exist or it it exists then it's ID was mention in "to" only,
      // which means that it should not contain the link with the next activity
      if (fromNode != null && fromNode.nextActivity != null)
        throw new FlowBuildException ("ActivityId provided to from() method already exists. ActivityId provided: " + 
            activityId + ". It is connected to ActivityId: " + fromNode.nextActivity.activityID);
      
      if (fromNode == null) 
      {
        fromNode = new FlowNode(activityId);
        nodes.put(activityId, fromNode);
      }
      
      this.latestFromNode = fromNode;
    }
    else
    {
      throw new FlowBuildException ("Wrong method invocation! from() can be called only after either to() or toEnd() method call. Found: " + buildState);
    }

    // Set the last method invoked as from()
    buildState = BuildState.FROM;
    
    return this;
  }

  public Flow<T> to(T activityId)
  {
    if (activityId == null) throw new FlowBuildException ("ActivityId cannot be null in \"to\"!");
    
    FlowNode      toNode, latestCondition;
    
    switch (buildState)
    {
      case FROM: case TO: case OTHERWISE:
        toNode = getOrCreateActivityNode(activityId);
        
        latestFromNode.nextActivity = toNode;
        latestFromNode = toNode;
        break;
        
      case ONLY_IF: case ELSE_IF: 
        toNode = getOrCreateActivityNode(activityId);
        latestCondition = openConds.peekLast();
        
        // Update the list of the next activities per condition 
        latestCondition.activityPerCondition.add(toNode);
        latestFromNode = toNode;
        
        break;
        
      default: 
        throw new FlowBuildException ("Wrong method invocation! to() can be called only after either from(), to(), onlyIf(), elseIf() or otherwise() method call. Found: " + buildState);
    }
    
    // Set the last method invoked as to()
    buildState = BuildState.TO;

    return this;
  }

  public Flow<T> onlyIf(T conditionId)
  {
    if (conditionId == null) throw new FlowBuildException ("ConditionId cannot be null in \"onlyIf\"!");
    
    FlowNode      condNode;
    
    switch (buildState)
    {
      case FROM: case TO: case OTHERWISE:
        // The current fromNode takes condition properties
        condNode = latestFromNode;
        
        break;
      
      // onlyIf() comes as nested for the previous onlyIf() or esleIf() calls
      case ONLY_IF: case ELSE_IF: 
        condNode = new FlowNode (null); // activityId=null indicates that this is nested condition
        
        // set this conditions as the next of the parent condition node
        FlowNode  openCond = openConds.peekLast();
        
        openCond.activityPerCondition.add(condNode);

        break;
        
      default: 
        throw new FlowBuildException ("Wrong method invocation! onlyIf() can be called only after from(), to(), onlyIf(), elseIf() or otherwise() method call. Found: " + buildState);
    }
    
    // Add the first condition ID for "onlyIf"
    condNode.conditionIDs = new ArrayList<T>(); 
    condNode.conditionIDs.add(conditionId);
    condNode.activityPerCondition = new ArrayList<FlowNode>();
    
    // Add condition node into the condition queue
    openConds.offerLast(condNode);
    
    // Set the last method invoked as onlyIf()
    buildState = BuildState.ONLY_IF;

    return this;
  }

  public Flow<T> elseIf(T conditionId)
  {
    if (conditionId == null) throw new FlowBuildException ("ConditionId cannot be null in \"elseIf\"!");
    
    if (isOnlyIfContext() &&
        (buildState == BuildState.TO || 
         buildState == BuildState.END || 
         buildState == BuildState.OTHERWISE))
    {
      openConds.peekLast().conditionIDs.add(conditionId);
    }
    else
    {
      throw new FlowBuildException ("Wrong method invocation! elseIf() can be called only after either to(), end(), or otherwise() method call if onlyIf() was invoked before. Found: " + buildState);
    }

    // Set the last method invoked as esleIf()    
    buildState = BuildState.ELSE_IF;
    
    return this;
  }
  
  public Flow<T> otherwise()
  {
    if (isOnlyIfContext() &&
        (buildState == BuildState.TO || 
         buildState == BuildState.END || 
         buildState == BuildState.OTHERWISE))
    {
      // Remove last condition from the queue since "otherwise" closes the condition and
      // set it's next activity to end node
      openConds.pollLast().nextActivity = endActivity;
    }
    else
    {
      throw new FlowBuildException ("Wrong method invocation! otherwise() without input can be called only after either to(), end(), or otherwise() method call if onlyIf() was invoked before. Found: " + buildState);
    }
    
    // Set the last method invoked as otherwise() with expected following toEnd()
    buildState = BuildState.OTHERWISE_END;
    
    return this;
  }
  
  public Flow<T> otherwise(T activityId)
  {
    if (activityId == null) throw new FlowBuildException ("ActivityId cannot be null in in \"otherwise\"!");
  
    if (isOnlyIfContext() &&
        (buildState == BuildState.TO || 
         buildState == BuildState.END || 
         buildState == BuildState.OTHERWISE))
    {
      FlowNode    toNode = getOrCreateActivityNode(activityId); 
      
      // Remove last condition from the queue since "otherwise" closes the condition and
      // set it's next activity to this node
      openConds.pollLast().nextActivity = toNode;
      
      // The latest fromNode is this toNode
      latestFromNode = toNode;
    }
    else
    {
      throw new FlowBuildException ("Wrong method invocation! otherwise() can be called only after either to(), end(), or otherwise() method call if onlyIf() was invoked before. Found: " + buildState);
    }
    
    // Set the last method invoked as otherwise()
    buildState = BuildState.OTHERWISE;
    
    return this;
  }

  public Flow<T> toEnd()
  {
    FlowNode      latestCondition;
    
    switch (buildState)
    {
      case OTHERWISE_END:
        // otherwise() without input already updated next activity for the latest from node
        break;
        
      case FROM: case TO: case OTHERWISE: 
        latestFromNode.nextActivity = endActivity;
        
        break;
        
      case ONLY_IF: case ELSE_IF: 
        latestCondition = openConds.peekLast();
        
        // Update the list of the next activities per condition 
        latestCondition.activityPerCondition.add(endActivity);
        
        break;
        
      default: 
        throw new FlowBuildException ("Wrong method invocation! to() can be called only after either from(), to(), onlyIf(), elseIf() or otherwise() method call. Found: " + buildState);
    }
 
    latestFromNode = endActivity;
    buildState = BuildState.END;
    isEndPresent = true;
    
    return this;
  }
  
  @Override
  public String toString()
  {
    StringBuilder   strBuilder = new StringBuilder();
    
    activityToString(startActivity, strBuilder, new HashSet<FlowNode>());
    
    return strBuilder.toString();
  }
  
  // Helpers
  
  protected void activityToString(FlowNode node, StringBuilder strBuilder, Set<FlowNode> uniques)
  {
    if (node == null || node == endActivity)  return;
    if (!uniques.add(node))                   return;
  
    if (node == startActivity)  strBuilder.append("START->");
    else                        strBuilder.append(node.activityID.toString()).append("->");
  
    if (node.conditionIDs != null)
    {
      final String  indent = "  ";
      
      strBuilder.append('\n');
      
      final int condSize = node.conditionIDs.size();
      
      for (int i = 0; i < condSize; i++)
      {
        T         condition = node.conditionIDs.get(i);
        FlowNode  condActivity = node.activityPerCondition.get(i);
        
        conditionToString(condition, condActivity, strBuilder, indent);
      }
      
      strBuilder.append(indent).append("otherwise->").
        append(
            node.nextActivity.activityID==null ? "END" : node.nextActivity.activityID.toString()).append('\n');
      
      // Print all non-conditional nodes, which were not printed
      // in the loop above
      nonConditionNodeToString(node, strBuilder, uniques);
    }
    else
    {
      if (node.nextActivity.activityID != null) 
        strBuilder.append(node.nextActivity.activityID.toString()).append('\n');
      else if (node.nextActivity == endActivity)
        strBuilder.append("END\n");
        
    }
    
    activityToString(node.nextActivity, strBuilder, uniques);
  }
  
  protected void nonConditionNodeToString(Flow<T>.FlowNode node, StringBuilder strBuilder, Set<FlowNode> uniques)
  {
    if (node != null)
    {
      if (node.activityPerCondition != null)
      {
        for (FlowNode condActivity: node.activityPerCondition)
        {
          nonConditionNodeToString(condActivity, strBuilder, uniques);
        }
      }
      else if (node.activityID != null)
      {
        if (uniques.add(node))
        {
          strBuilder.append(node.activityID.toString()).append("->");
          if (node.nextActivity.activityID != null)
          {
            strBuilder.append(node.nextActivity.activityID.toString()).append('\n');
          }
          else
          {
            strBuilder.append("END\n");
          }
        }
      }
      
      nonConditionNodeToString(node.nextActivity, strBuilder, uniques);
    }
  }

  protected void conditionToString(T condition, FlowNode condActivity, StringBuilder strBuilder, String ident)
  {
    strBuilder.append(ident).append("if[").append(condition.toString()).append("]->");
    
    if (condActivity.activityID == null)  // then it's nested condition or END and we need to go deep
    {
      if (condActivity != endActivity)
      {
        strBuilder.append('\n');
        nestedConditionToString(condActivity, strBuilder, ident + ident);
      }
      else strBuilder.append("END");
    }
    else
    {
      strBuilder.append(condActivity.activityID.toString());
    }
    
    strBuilder.append('\n');
  }

  protected void nestedConditionToString(FlowNode nestedCondition, StringBuilder strBuilder, String ident)
  {
    if (nestedCondition.conditionIDs != null)
    {
      int condSize = nestedCondition.conditionIDs.size();
      
      for (int i = 0; i < condSize; i++)
      {
        T         condition = nestedCondition.conditionIDs.get(i);
        FlowNode  condActivity = nestedCondition.activityPerCondition.get(i);
        
        conditionToString(condition, condActivity, strBuilder, ident);
      }
    }
    
    strBuilder.append(ident).append("otherwise->");
    if (nestedCondition.nextActivity.activityID == null)
    {
      if (nestedCondition.nextActivity == endActivity) strBuilder.append("END");
    }
    else
    {
      strBuilder.append(nestedCondition.nextActivity.activityID.toString());
    }
  }

  protected FlowNode getOrCreateActivityNode(T activityId)
  {
    FlowNode    node = nodes.get(activityId); 
    
    if (node == null) 
    {
      node = new FlowNode(activityId);
      nodes.put(activityId, node);
    }
    
    return node;
  }
  
  protected boolean isOnlyIfContext()
  {
    return openConds.size() > 0;
  }
  
  protected class FlowInstanceImpl implements FlowInstance<T>
  {
    protected Map<T,FlowOperation<T>> operationMap = null;
    protected FlowOperation<T>        defaultOp = null;
    protected FlowNode                currentNode = startActivity;
    
    public FlowInstanceImpl (FlowOperation<T> operation)
    {
      this.defaultOp = operation;
    }
    
    public FlowInstanceImpl (Map<T,FlowOperation<T>> operationMap)
    {
      this.operationMap = operationMap;
    }
    
    public FlowInstanceImpl (Map<T,FlowOperation<T>> operationMap, FlowOperation<T> defaultOp)
    {
      this.operationMap = operationMap;
      this.defaultOp = defaultOp;
    }

    @Override
    public boolean move()
    {
      if (isFinished()) return false;
        
      FlowNode    from = currentNode;
      FlowNode    to = null;
      
      if (currentNode.conditionIDs == null)   // then it's non-conditional transition
      {
        to = currentNode.nextActivity;
      }
      else                                    // transition is conditional
      {
        to = getDestinationByCondition(from);
        
        // If the next activity is conditional then 
        // we need to execute conditions again
        while(to != endActivity && to.activityID == null)
        {
          to = getDestinationByCondition(to);
        }
      }
      
      currentNode = to;
      
      if (from == startActivity)
      {
        FlowOperation<T>  op = getOperation(to);
        
        if (op != null) op.fromStart(to.activityID);
      }
      else
      {
        if (currentNode == endActivity)
        {
          FlowOperation<T>  op = getOperation(from);
          
          if (op != null) op.toEnd(from.activityID);
        }
        else
        { 
          // If the last node is reached then set end activity forcibly 
          if (currentNode.nextActivity == endActivity) currentNode = endActivity;
          
          FlowOperation<T>  op = getOperation(to);
          
          if (op != null) 
          {
            op.changed(from.activityID, to.activityID);
            
            if (isFinished()) op.toEnd(to.activityID);
          }
        }
      }
      
      return true;
    }
    
    @Override
    public boolean isFinished()
    {
      return currentNode == endActivity;
    }

    @Override
    public void reset()
    {
      currentNode = startActivity;
    }
    
    @Override
    public boolean resetTo(T activiy)
    {
      FlowNode    jumpNode = nodes.get(activiy); 
      boolean     isJumped = false;
      
      if (jumpNode != null)
      {
        currentNode = jumpNode; 
        isJumped = true;
      }
      
      return isJumped;
    }
    
    protected FlowOperation<T> getOperation (FlowNode node)
    {
      FlowOperation<T>  op = null;
      
      if (node != null) op = getOperation (node.activityID);
      
      return op;
    }
    
    protected FlowOperation<T> getOperation (T nodeId)
    {
      FlowOperation<T>  op = null;
      
      if (nodeId != null)
      {
        if (operationMap != null) op = operationMap.get(nodeId);
        if (op == null)           op = defaultOp;
      }

      return op;
    }
    
    protected FlowNode getDestinationByCondition (FlowNode from)
    {
      FlowNode    to = null;
      int         condSize = from.conditionIDs.size();
      
      for (int i = 0; i < condSize; i++)
      {
        T             condId = from.conditionIDs.get(i);
        FlowOperation<T>  cond = getOperation(condId);
        
        if (cond != null && cond.test(condId))
        {
          to = from.activityPerCondition.get(i);
          break;
        }
      }
      
      // If no condition was satisfied then do default        
      if (to == null) to = from.nextActivity;
      
      return to;
    }
  }
}

