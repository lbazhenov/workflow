package leonid.flow.workflow;

/**
 * This interface must be implemented and supplied to <code>getFlowInstance</code> invocation to be notified
 * for every flow transition. It's possible to provide the single implementation for all transition or
 * just for subset of transition along with providing default implementation. If default implementation is 
 * not provide while covering only subset of transitions then for uncovered transitions no notifications will
 * be thrown. 
 * 
 * @author Leonid Bazhenov
 * @version 1.0
 */
public interface FlowOperation<T>
{
  void fromStart(T to);
  void changed(T from, T to);
  void toEnd(T from);
  
  boolean test (T condition);
}