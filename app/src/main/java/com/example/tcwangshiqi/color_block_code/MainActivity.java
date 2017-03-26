package com.example.tcwangshiqi.color_block_code;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


public class MainActivity extends AppCompatActivity {

    public static final int WHITE = 0, RED = 1, GREEN = 2, BLUE = 3, BLACK = 100, UNKNOWN = 50;

    public static final int MAXLINE = 100, MAXCOLUMN = 100;

    /* Line check means only continuous LINECHECKNUM same color could we make sure this is
     * one color we need to record or judge
     * Column check means if we get the same sequence of color for continuous COLUMNCHECKNUM
     * we could make sure that we read this line correctly.
     */
    public static final int LINECHECKNUM = 15, COLUMNCHECKNUM = 20;

    public static final int LINEOPTIMIZATIONNUM = 3 * LINECHECKNUM;

    /* ID_NUM is how many blocks stand for ID
     * HASHNUM means how many blocks stand for hash
     * ID_LENGTH means how many blocks stand for the number of ID blocks (ID_NUM).
     * PADDING_NUM means how many codes are padding following with ID
     * HASH_NUM means how many codes stand for hash check.
     * HASH_BLOCKS_NUM means how many codes of ID used in one block for one hash check code.
     */
    public static final int ID_LENGTH = 6, ID_NUM = 40, PADDING_NUM = 2, HASH_NUM = 6, HASH_BLOCKS_NUM = 7;

    /* record the numbers for convenience */
    public int RedNum = 0, GreenNum = 0, BlueNum = 0, BlackNum = 0;

    /* record number of volume */
    public int line = 0, col = 0;

    /* record every color of color-block code, it starts with record[1][1]*/
    int[][] record = new int[MAXLINE][MAXCOLUMN];

    /* record the new color after coordinate transformation */
    int[][] record2 = new int[MAXLINE][MAXCOLUMN];

    /* record the new sequence of ID information, starting from record[0] */
    int[] record3 = new int[MAXLINE * MAXCOLUMN];

    /*  the array records the id */
    int[] id = new int[ID_NUM];

    /* the array records the id length */
    int[] id_length = new int[ID_LENGTH];

    /* the array records the hash codes */
    int[] hash = new int[HASH_NUM];
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    /* to check whether this is the id we need */
    public boolean is_needed_id = false;

    /* to check whether this id is needed to be checked,
     * only true after user submits the target id
     */
    public boolean needed_check_id = false;

    /* the string type of ID we needed, which is got from EditText */
    String id_needed;

    EditText mEdit;
    Button mButton;
    ImageView img;

    /* useing map to simulate the database for storing the book ID value */
    public static Map<String,String> dataBase = new HashMap<String, String>();

    private View.OnClickListener imageListener = new View.OnClickListener() {
        public void onClick(View v) {
            // do something when the button is clicked
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        registerBooks();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    @Override
    protected void onResume() {
        super.onResume();
        getPixColor();
        parseCodes();
        img = (ImageView) findViewById(R.id.imageView1);
        mButton = (Button)findViewById(R.id.button_edit);
        mEdit   = (EditText)findViewById(R.id.mEdit);

        mButton.setOnClickListener(
                new View.OnClickListener()
                {
                    public void onClick(View view)
                    {
                        id_needed = searchDataBase(mEdit.getText().toString());
                        needed_check_id = true;
                        is_needed_id = checkId();
                        if(is_needed_id) {
                            img.setBackgroundColor(Color.GREEN);
                            Toast.makeText(MainActivity.this, "This is the book " + mEdit.getText().toString(), Toast.LENGTH_SHORT).show();
                            //image.setOnClickListener(imageListener);
                        }
                        else {
                            Toast.makeText(MainActivity.this, "Oops, this is not the book " + mEdit.getText().toString(), Toast.LENGTH_SHORT).show();
                            img.setBackgroundColor(Color.WHITE);
                        }
                    }
                });
    }

    /* This function is used to register new books */
    public void registerBooks() {
        dataBase.put("Harry Potter", "3212123231223132313313231231223121332311");
        dataBase.put("The Great Gatsby", "3212123231233112333313231231223121332212");
        dataBase.put("Hunger Game", "3222121231233122133313231231223121332222");
    }

    public String searchDataBase(String bookName) {
        for (Map.Entry<String, String> entry : dataBase.entrySet()) {
            if(entry.getKey().equals(bookName)) {
                System.out.println("find the book " + entry.getKey() + " and its id is " + entry.getValue());
                return entry.getValue();
            }
        }
        System.out.println("Could not find the book " + bookName);
        Toast.makeText(MainActivity.this, "No such a book in DataBase!", Toast.LENGTH_SHORT).show();
        return null;
    }

    public void test(View view) {
        Toast.makeText(MainActivity.this, "Red: " + RedNum + " Green: " + GreenNum + " Blue: " + BlueNum + "Black:" + BlackNum + " line: " + line + " col:" + col, Toast.LENGTH_LONG).show();
    }

    public void test1(View view) {
        for (int i = 0; i < line; i++) {
            String str = "line" + (i + 1) + ":";
            for (int j = 0; j < col; j++) {
                str = str + record2[i + 1][j + 1];
            }
            Toast.makeText(MainActivity.this, str, Toast.LENGTH_SHORT).show();
        }
    }

    /* to check whether the id of this code is the same as what we want */
    public boolean checkId() {
        boolean is_needed_id =false;
        String id_str;

        id_str = Arrays.toString(id)
                .replace(",", "")  //remove the commas
                .replace("[", "")  //remove the right bracket
                .replace("]", "")  //remove the left bracket
                .replace(" ", "")  //remove the left bracket
                .trim();           //remove trailing spaces from partially initialized arrays;
       if(id_str.equals(id_needed)) {
           System.out.println("This is the code we want to find!");
           is_needed_id = true;
       }
        return is_needed_id;
    }

    /*
     * turn the record2[][] into a record3[] for convenience.
     * Also removing the signal codes and location codes.
     */
    public void turnToSeq() {
        int n = 0;
        for (int i = 1; i < line + 1; i++) {
            for (int j = 1; j < col + 1; j++) {
                if ((i == 1 && j == 1) || (i == line && j == 1) || (i == 1 && j == col)) ;
                else if ((i == 1 && j == 2) || (i == 1 && j == 3) || (i == 1 && j == 4)) ;
                else {
                    record3[n] = record2[i][j];
                    n++;
                }
            }
        }
    }

    /*
     * print function for simplification.
     */
    public void printArray(int[] array, int length) {
        for (int i = 0; i < length; i++) {
            System.out.print(array[i]);
        }
        System.out.println();
    }

    /*
     * parsing the record2[][] into specific structure
     * including ID, ID length, hash.
     */
    public void parseCodes() {
        signalCodesCheck();
        turnToSeq();
        System.out.print("record3:");
        printArray(record3, col * line);
        int n = 0;
        while (record3[n] != WHITE) {
            if (n < ID_LENGTH) {
                id_length[n] = record3[n];
            }
            if (n >= ID_LENGTH && n < ID_NUM + ID_LENGTH) {
                id[n - ID_LENGTH] = record3[n];
            }
            if (n >= ID_NUM + ID_LENGTH + PADDING_NUM && n < ID_NUM + ID_LENGTH + HASH_NUM + PADDING_NUM) {
                hash[n - ID_NUM - ID_LENGTH - PADDING_NUM] = record3[n];
            }
            n++;
        }
        System.out.print("ID: ");
        printArray(id, ID_NUM);
        System.out.print("ID Length: ");
        printArray(id_length, ID_LENGTH);
        System.out.print("Hash: ");
        printArray(hash, HASH_NUM);
        idLengthCheck();
        hashCheck();
    }

    /*
     * show the current Id we have recognized.
     */
    public void showCurrentId(View view) {
        String id_str = "ID = ";
        for (int n = 0; n < ID_NUM; n++) {
            id_str = id_str + id[n];
        }
        Toast.makeText(MainActivity.this, id_str, Toast.LENGTH_SHORT).show();
    }

    /*
     * check the signal codes, only succeeds when the signal codes are GGR
     */
    public void signalCodesCheck() {
        if (record2[1][2] != GREEN || record2[1][3] != GREEN || record2[1][4] != RED) {
            Toast.makeText(MainActivity.this, "Signal Codes Error!", Toast.LENGTH_SHORT).show();
        } else {
            System.out.println("signal codes succeeds!");
        }
    }

    /*
     * check the id length, only succeeds when id length is equal to ID_NUM
     * When the color-blocks codes are used for information like ID, hash and id_length
     * the red means 0, green is 1 and blue is 2 but stored as (r1, g2, b3) to give 0 to WHITE.
     * THIS IS THE PLACE WE NEED TO LOOK OUT!
     */
    public void idLengthCheck() {
        int current_length = 0;
        int p = 1;
        for (int i = 0; i < ID_LENGTH; i++) {
            current_length = current_length + p * (id_length[ID_LENGTH - i - 1] - 1);
            p = p * 3;
        }
        if (current_length == ID_NUM) {
            System.out.println("id length check succeeds!");
        } else {
            System.out.println("id length check error, is: !" + current_length);
        }
    }

    /*
     * check the hash of the ID to ensure the ID is read correctly.
     * we use one code for checking every HASH_BLOCKS_NUM(default is 7).
     * The idea is familiar with Parity check code.
     */
    public void hashCheck() {
        int[] current_hash = new int[HASH_NUM];
        for (int i = 0; i < HASH_NUM; i++) {
            int red_num = 0, green_num = 0, blue_num = 0;
            for (int j = 0; j < HASH_BLOCKS_NUM; j++) {
                if (id[j + i] == RED) {
                    red_num++;
                }
                if (id[j + i] == GREEN) {
                    green_num++;
                }
                if (id[j + i] == BLUE) {
                    blue_num++;
                }
            }
            current_hash[i] = red_num * (RED - 1) + green_num * (GREEN - 1) + blue_num * (BLUE - 1);
            if (current_hash[i] % 3 != (hash[i] - 1)) {
                System.out.println("Hash check error at" + (i + 1) + "!");
                return;
            }
        }
        System.out.println("Hash check succeeds!");
    }

    /* adjust the codes direction if not in a correct way */
    public void adjustDirection() {
        if (record[1][1] == GREEN && record[1][col] == RED && record[line][col] == BLUE) {
            for (int i = 1; i < line + 1; i++) {
                for (int j = 1; j < col + 1; j++) {
                    record2[i][j] = record[i][j];
                }
            }
        }

        if (record[1][1] == BLUE && record[line][col] == GREEN && record[line][1] == RED) {//原图像的逆时针旋转图
            int line2 = col;
            int col2 = line;
            //x1 = y , y1 = col-x+1  x=i,y=j
            for (int i = 1; i < line + 1; i++) {
                for (int j = 1; j < col + 1; j++) {
                    record2[j][col2 - i + 1] = record[i][j];
                }
            }
            line = line2;
            col = col2;
        }

        if (record[1][col] == GREEN && record[line][1] == BLUE && record[line][col] == RED) {//再逆时针九十度
            //x1 = line-x+1 , y1 = col-y+1
            for (int i = 1; i < line + 1; i++) {
                for (int j = 1; j < col + 1; j++) {
                    record2[line - i + 1][col - j + 1] = record[i][j];
                }
            }
        }

        if (record[1][1] == GREEN && record[1][col] == RED && record[line][col] == BLUE) {//再逆时针九十度
            int line2 = col;
            int col2 = line;

            //x1 = line-y+1 , y1 = x
            for (int i = 1; i < line + 1; i++) {
                for (int j = 1; j < col + 1; j++) {
                    record2[line2 - j + 1][i] = record[i][j];
                }
                System.out.println();
            }
            line = line2;
            col = col2;
        } else {
            Toast.makeText(MainActivity.this, "Location Codes Error!", Toast.LENGTH_SHORT).show();
        }
    }

    public boolean isBlue(int r, int g, int b) {
        if ((b > g + 80) && (b > r + 80) && (g < 120) && (r < 120)) {
            return true;
        } else return false;
    }

    public boolean isRed(int r, int g, int b) {
        if ((r > g + 80) && (r > b + 80) && (g < 120) && (b < 120)) {
            return true;
        } else return false;
    }

    public boolean isGreen(int r, int g, int b) {
        if ((g > b + 80) && (g > r + 80) && (b < 120) && (r < 120)) {
            return true;
        } else return false;
    }

    public boolean isWhite(int r, int g, int b) {
        if ((g > 140) && (b > 140) && (r > 140)) {
            return true;
        } else return false;
    }

    public boolean isBlack(int r, int g, int b) {
        if ((g < 10) && (b < 10) && (r < 10)) {
            return true;
        } else return false;
    }

    public boolean checkHash(int[] hash) {
        for (int i = 0; i < LINECHECKNUM - 1; i++) {
            if (hash[0] != hash[i]) {
                return false;
            }
        }
        return true;
    }

    public int checkColor(int r, int g, int b) {
        if (isWhite(r, g, b)) return WHITE;
        if (isRed(r, g, b)) return RED;
        if (isGreen(r, g, b)) return GREEN;
        if (isBlue(r, g, b)) return BLUE;
        if (isBlack(r, g, b)) return BLACK;
        else return UNKNOWN;
    }

    /*
     * The key function to read the code via reading the pix one by one
     * Make the App recognize different colors and parse them into numbers that
     * the computer could handle.
     */
    public void getPixColor() {
        Bitmap src = BitmapFactory.decodeResource(getResources(), R.drawable.book_code3);
        int R, G, B;
        int pixelColor;
        int height = src.getHeight();
        int width = src.getWidth();
        /* to see whether this line is a white line. true means yes */
        boolean judgeWhite;
        /* whether this color needed to be printed */
        boolean neededPrintLine = true;
        /* whether this line has been printed */
        boolean hasPrintedLine = false;
        /* whether this line is needed to be parsed and record */
        boolean neededRecordLine = false;
        /* the hash for every line, the older, the bigger index */
        int[] hash = new int[LINECHECKNUM];
        /* whether the later lines are needed to be optimized */
        boolean lineOptimization = false;

        //drawFrame();

        System.out.println("height:" + height + "width:" + width);

        for (int y = 0; y < height; y++) {
            /* Current Color */
            int currentColor = WHITE;
            /* last Color, w0, r1, g2. b3, black100, unknown 50*/
            int lastColor = WHITE;
             /* current column */
            int currentCol = 0;
            /* the temporary record for one line */
            int[] K = new int[MAXCOLUMN];
            /* it means whether this color needed to be checked.
             * usually if the current color is equal to the last color, it will pass.
             * but if this color is going to be recorded, we need to ensure
             * whether this color remains the same for COLUMNCHECKNUM times.
             * otherwise, it should not be recorded!
             */
            boolean neededCheckCol = false;
            /* the temporary record for one color */
            int[] T = new int[COLUMNCHECKNUM];

            /* first assume this line is white */
            judgeWhite = true;
            for (int i = LINECHECKNUM - 1; i > 0; i--) {
                hash[i] = hash[i - 1];
            }
            hash[0] = 0;
            for (int x = 0; x < width; x++) {
                pixelColor = src.getPixel(x, y);
                R = Color.red(pixelColor);
                G = Color.green(pixelColor);
                B = Color.blue(pixelColor);
                currentColor = checkColor(R, G, B);
                if (currentColor == UNKNOWN || currentColor == BLACK) {
                    continue;
                }
                if (currentColor == lastColor) {
                    continue;
                }
                if (currentColor != WHITE) {
                    judgeWhite = false;
                    if (neededPrintLine) {
                        neededRecordLine = true;
                    }
                }
                if (neededRecordLine) {
                    if (currentColor == RED) {
                        K[currentCol + 1] = RED;
                        hash[0] += 1;
                        currentCol++;
                    }
                    if (currentColor == GREEN) {
                        K[currentCol + 1] = GREEN;
                        hash[0] += 10;
                        currentCol++;
                    }
                    if (currentColor == BLUE) {
                        K[currentCol + 1] = BLUE;
                        hash[0] += 100;
                        currentCol++;
                    }
                }
                lastColor = currentColor;
                if (currentCol + 1 >= MAXCOLUMN) continue;
            }
            // the end of parsing for one line
            if (checkHash(hash) && neededRecordLine && neededPrintLine) {
                System.out.println(line);
                for (int i = 1; i < currentCol + 1; i++) {
                    record[line + 1][i] = K[i];
                    System.out.println("line" + (line + 1) + " column" + i + ": " + K[i]);
                    if (K[i] == RED) RedNum++;
                    else if (K[i] == GREEN) GreenNum++;
                    else if (K[i] == BLUE) BlueNum++;
                    else BlackNum++;
                }
                hasPrintedLine = true;
                neededRecordLine = false;
                lineOptimization = true;
                line++;
            }
            /* priority sequence is: neededprintline->neededrecordline->hasprintedline */
            if (hasPrintedLine) {
                neededRecordLine = false;
                neededPrintLine = false;
                currentCol = 0;
            }
            if (line + 1 >= MAXLINE) continue;
            if (judgeWhite) {
                neededPrintLine = true;
                neededRecordLine = false;
                hasPrintedLine = false;
                lineOptimization = false;
                //System.out.println("white line, update!");
            }
            if (lineOptimization) {
                y += LINEOPTIMIZATIONNUM;
            }
        }
        col = (RedNum + BlueNum + GreenNum + BlackNum) / line;
        adjustDirection();
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Main Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }

    public class HKImageView extends ImageView {

        public HKImageView(Context context, AttributeSet attrs) {
            super(context, attrs, 0);
        }

        public HKImageView(Context context) {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            // 画边框
            Rect rect1 = getRect(canvas);
            Paint paint = new Paint();
            paint.setColor(Color.GRAY);
            paint.setStyle(Paint.Style.STROKE);

            // 画边框
            canvas.drawRect(rect1, paint);

            paint.setColor(Color.LTGRAY);

            // 画一条竖线,模拟右边的阴影
            canvas.drawLine(rect1.right + 1, rect1.top + 2, rect1.right + 1,
                    rect1.bottom + 2, paint);
            // 画一条横线,模拟下边的阴影
            canvas.drawLine(rect1.left + 2, rect1.bottom + 1, rect1.right + 2,
                    rect1.bottom + 1, paint);

            // 画一条竖线,模拟右边的阴影
            canvas.drawLine(rect1.right + 2, rect1.top + 3, rect1.right + 2,
                    rect1.bottom + 3, paint);
            // 画一条横线,模拟下边的阴影
            canvas.drawLine(rect1.left + 3, rect1.bottom + 2, rect1.right + 3,
                    rect1.bottom + 2, paint);
        }

        Rect getRect(Canvas canvas) {
            Rect rect = canvas.getClipBounds();
            rect.bottom -= getPaddingBottom();
            rect.right -= getPaddingRight();
            rect.left += getPaddingLeft();
            rect.top += getPaddingTop();
            return rect;
        }
    }
}


