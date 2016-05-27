package leonid.flow.workflow;

/**
 * This exceptions may be thrown during flow building process due to method invocation sequence violation. 
 * 
 * @author Leonid Bazhenov
 * @version 1.0
 */

public class FlowBuildException extends RuntimeException
{
  private static final long serialVersionUID = 8793203367636285473L;

  public FlowBuildException()
  {
    super();
  }

  public FlowBuildException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace)
  {
    super(message, cause, enableSuppression, writableStackTrace);
  }

  public FlowBuildException(String message, Throwable cause)
  {
    super(message, cause);
  }

  public FlowBuildException(String message)
  {
    super(message);
  }

  public FlowBuildException(Throwable cause)
  {
    super(cause);
  }
}
