package leonid.flow.workflow;

/**
 * The implementation of this interface is returned by flow builder to operate the flow. 
 * 
 * @param <T> - data type of the flow node ID. 
 * 
 * @author Leonid Bazhenov
 * @version 1.0
 */
public interface FlowInstance<T>
{
  boolean move ();
  boolean isFinished();
  boolean resetTo(T activiy);
  void reset();
}
