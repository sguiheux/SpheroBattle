package fr.sgu.spherobattle.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StreamCorruptedException;

public class Message implements Serializable{
    public TypeMessage type;
    public String data;
    public long ts;
    public double speed;
    public int result;

    public static void main(String... a){
        Message msg = new Message();
        msg.type = TypeMessage.PRESENCE;
        msg.data = "1";
        System.out.println(toByte(msg));
    }

    public static byte[] toByte(Message msg) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream os = null;
        try {
            os = new ObjectOutputStream(out);
            os.writeObject(msg);
            return out.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Message toMessage(byte[] b){
        ByteArrayInputStream in = new ByteArrayInputStream(b);
        ObjectInputStream is = null;
        try {
            is = new ObjectInputStream(in);
            return (Message) is.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }
}
