package io.zeebe.client.cmd;

public class ClientException extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    public ClientException(String message)
    {
        super(message);
    }

    public ClientException(String message, Exception cause)
    {
        super(message, cause);
    }

}
