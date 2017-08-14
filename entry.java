import java.util.LinkedList;

public class entry {
int valid=0;
String tag;
String[] data;
String blockoffset;
int set;
int dirty;
int memloc;
int bo;
public entry(){
	valid = 0;
	dirty = 0;
}
public entry(String[] val,String tg,int mmloc, int b){
	data = val;
	valid = 1;
	tag = tg;
	dirty = 0;
	memloc=mmloc;
	bo = b;

}

}
