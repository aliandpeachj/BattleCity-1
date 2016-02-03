package bert.young;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.widget.LinearLayout;
import android.view.MotionEvent;
import bert.young.GameView.GameThread;
import bert.young.Movable.Dir;
import bert.young.UserAction.UserActionType;


// ������Դ
enum AudioID {
    AUDIO_OPENING,
    AUDIO_FIRE,
    AUDIO_HIT,
    AUDIO_BONUS,
    AUDIO_BLAST,
    AUDIO_OVER,
    AUDIO_LIFE,
    
    AUDIO_MAX,
}


final class Misc
{
    private static boolean    mVibrate;
    private static boolean    mMusic;
    private static int        mDefaultLife;
    private static int        mDefaultStage;

    private static Vibrator     mVibrator;
    private static AudioManager mAudioMgr;
    private static int[]    mSoundId = new int [AudioID.AUDIO_MAX.ordinal()];

    static boolean Init() {
        Activity  activity = GameActivity.Instance();
        activity.setVolumeControlStream(AudioManager.STREAM_MUSIC);

        mAudioMgr = (AudioManager)activity.getSystemService(Context.AUDIO_SERVICE);
        AudioPool.Create();
        mSoundId[0] = AudioPool.Load(activity, R.raw.opening);
        mSoundId[1] = AudioPool.Load(activity, R.raw.fire);
        mSoundId[2] = AudioPool.Load(activity, R.raw.hit);
        mSoundId[3] = AudioPool.Load(activity, R.raw.bonus);
        mSoundId[4] = AudioPool.Load(activity, R.raw.blast);
        mSoundId[5] = AudioPool.Load(activity, R.raw.gameover);
        mSoundId[6] = AudioPool.Load(activity, R.raw.bonus_life);
        
        mDefaultLife    = activity.getIntent().getIntExtra(BattleCity.KEY_LIFE, 3);
        mMusic          = activity.getIntent().getBooleanExtra(BattleCity.KEY_MUSIC, true);
        mVibrate        = activity.getIntent().getBooleanExtra(BattleCity.KEY_VIBRATE, true);
        mDefaultStage   = activity.getIntent().getIntExtra(BattleCity.KEY_STAGE, 1);
        mVibrator       = (Vibrator)activity.getSystemService(Service.VIBRATOR_SERVICE);

        return  true;
    }

    private static int GetCurVolume( ) {
        return  mAudioMgr.getStreamVolume(AudioManager.STREAM_MUSIC);
    }
    
    static void PlaySound(AudioID  id) {
        if (mMusic)
            AudioPool.Play(mSoundId[id.ordinal()], GetCurVolume(), 0);
    }
    
    static int GetDefaultStage() {
        return mDefaultStage;
    }
    
    static int GetDefaultLife() {
        return mDefaultLife;
    }

    static void Vibrate(long  ms) {
        if (mVibrate && null != mVibrator)
            mVibrator.vibrate(ms);
    }

    static void UnInit() {
        AudioPool.Destroy();
        for (int i = 0; i < mSoundId.length; ++ i) {
            mSoundId[i] = -1;
        }
    }
}


public class GameActivity extends Activity implements OnTouchListener {
    private static final String  TAG = "GameActivity";

    // Ŀǰ�ò��������Ժ�洢��Ϸ����ʱʹ��
    //private GameThread m_thread = null; 

    public static GameActivity  Instance() {
        return  smGameAct;
    }
    private static  GameActivity   smGameAct;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        smGameAct = this;
        Misc.Init();
        
        // ȫ������
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        // ���ֿ��ǣ�
        // �������������Բ���
        // ��� �� ��񲼾֡��������
        // �м� �� ��Ϸ������GameView
        // �Ҳ� �� ���Բ��֡���������ť���ӵ�����ͣ���˳�
        LinearLayout   rootLayout = (LinearLayout)LayoutInflater.from(this).inflate(R.layout.game, null);
        if (null == rootLayout) {
            Log.e(TAG, "Can not find game layout");
            finish();
        }
    
        setContentView(rootLayout);
            
        DisplayMetrics metric = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metric);
        final int width  = metric.widthPixels;     // ��Ļ��ȣ����أ�
        final int height = metric.heightPixels;    // ��Ļ�߶ȣ����أ�
        Log.d(TAG, "Screen width = "  + width);
        Log.d(TAG, "Screen height = " + height);
        //float density  = metric.density;      // ��Ļ�ܶȣ�0.75 / 1.0 / 1.5��
        //int densityDpi = metric.densityDpi;  // ��Ļ�ܶ�DPI��120 / 160 / 240��
        
        // ��Ϸ�����������Σ��պ�װ��IMAGE_NUM��ͼƬ
        // 2013.1.29 Ϊ�˲�ʹ��float���꣬������STEP��4�ı�����STEP/4��Ϊ�ߴ������Ԫ
        final int  marginTotal = height % (2 * Map.IMAGE_NUM * 8);
        final int  worldSize   = height - marginTotal;
        Log.d(TAG, "Gameworld size " + worldSize);
        // world size = step * 4 * 13;
        LinearLayout.LayoutParams  params  =  new LinearLayout.LayoutParams(worldSize, worldSize);  
        params.leftMargin =  params.topMargin = params.rightMargin = params.bottomMargin = marginTotal / 2;  
        GameView view = (GameView) rootLayout.findViewById(R.id.gameview);
        view.setLayoutParams(params);

        // ע�ᰴť�¼�    
        View padBtn = this.findViewById(R.id.pad);
        MyResource.Assert(null != padBtn, "Can not find pad button");
        
        padBtn.setOnTouchListener(this);
        padBtn.setFocusable(true);
        padBtn.setFocusableInTouchMode(true);      

        View fireBtn = this.findViewById(R.id.fire);
        MyResource.Assert(null != fireBtn, "Can not find fire button");

        fireBtn.setOnTouchListener(this);
        fireBtn.setFocusable(true);
        fireBtn.setFocusableInTouchMode(true);
    }

    @Override
    public  void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        mInit = false;
    }
    
    @Override
    public  void onPause() {
        Log.d(TAG, "OnPause");
        mInit = false;
        TimerManager.Instance().KillAll();
        Misc.UnInit();
        GameWorld.Instance().ResetPlayer();
        super.onPause();
    }
    
    private boolean mInit       = false;
    private Rect    mPadRect    = new Rect();
    private Rect    mInvalidRect= new Rect(); // ����������ľ��β�������
    private Point   mPadCenter  = new Point();
    private Rect    mFireRect   = new Rect();
    // ��¼���µĵ�һ����ť�����Ͻ����ꣻ��Ϊ���µĵڶ�������㽫����Ϊԭ�������(0, 0)
    private int  mOffsetX ;
    private int  mOffsetY ;
    private int  mMoveCnt;
    
    enum  ButtonType {
        NONE,
        UP,
        DOWN,
        LEFT,
        RIGHT,
        FIRE;
        
        static Dir Convert2Dir(ButtonType type) {
            switch (type) {
            case UP:      return Dir.UP;
            case DOWN:    return Dir.DOWN;
            case LEFT:    return Dir.LEFT;
            case RIGHT:   return Dir.RIGHT;
            default:      return Dir.NONE;
            }
        }
    };
    
    private ButtonType GetButton(float xx, float yy) {
        int x = (int)xx;
        int y = (int)yy;

        if (mInvalidRect.contains(x, y)) {
            return   ButtonType.NONE;
        }

        if (mPadRect.contains(x, y)) {
            final int  deltaX = x - mPadCenter.x;
            final int  deltaY = y - mPadCenter.y;
            
            final int absDeltaX = deltaX > 0 ? deltaX : -deltaX;
            final int absDeltaY = deltaY > 0 ? deltaY : -deltaY;
            
            if (absDeltaX > absDeltaY) {
                // ������ˮƽ�ḽ��
                if (deltaX > 0) {
                    return     ButtonType.RIGHT;
                } else {
                    return     ButtonType.LEFT;
                }
            } else {
                // ��������ֱ�ḽ��
                if (deltaY < 0) {
                    return     ButtonType.UP;
                } else {
                    return     ButtonType.DOWN;
                }
            }
        }
        
        if (mFireRect.contains(x, y)) {
            return  ButtonType.FIRE;
        }

        Misc.Vibrate(60);
        return  ButtonType.NONE;
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        if (!mInit) {
            mInit = true;

            View   padBtn = this.findViewById(R.id.pad);
            if (!padBtn.getGlobalVisibleRect(mPadRect, new Point())) {
                Log.e(TAG, "Error get rect");
            }
            
            // ��ȡ��������ĵ�  Բ��
            mPadCenter.x = mPadRect.left + (mPadRect.right - mPadRect.left) / 2;
            mPadCenter.y = mPadRect.top + (mPadRect.bottom - mPadRect.top) / 2;
            
            final int radius = 15;
            mInvalidRect.top = mPadCenter.y - radius;
            mInvalidRect.bottom = mPadCenter.y + radius;
            mInvalidRect.left   = mPadCenter.x - radius;
            mInvalidRect.right  = mPadCenter.x + radius;

            View  fBtn = this.findViewById(R.id.fire);
            fBtn.getGlobalVisibleRect(mFireRect, new Point());
        }

        final GameThread thread = GameView.Instance().GetThread();
        final PlayerTank  pMe = GameWorld.Instance().GetMe();
        if (null == thread || null == pMe || !pMe.IsControllable()) {
            return  true;
        }

        final int viewId = view.getId();
        final int action = event.getActionMasked() & MotionEvent.ACTION_MASK;
        switch (action) {
        case MotionEvent.ACTION_MOVE:
            // 10��MOVE��Ϣ����һ��
            if (++ mMoveCnt % 10 != 0) {
                return true;
            }
            break;
            
        case MotionEvent.ACTION_UP:
            mMoveCnt = 0;
            break;
        }

        boolean   isHandled = true;
        final int actionIndex = event.getActionIndex();

        switch (action) {
        case MotionEvent.ACTION_UP:
            if (pMe.mDir != Dir.NONE) {
                mOffsetX = mOffsetY = 0;
                
                UserAction  act = new UserAction(UserActionType.ACTION_UP);
                thread.PushAction(act);
            }
             // �����view�д��󣺼����Ȱ�ס�����ɣ��ٰ��ӵ����ɣ�
            // �����ɿ���������ɿ��ӵ�����ô���ӵ���ť������õ��ͷ�
            // ���ڲ�ȷ�����ĸ�view����˶��ͷż���;            
            findViewById(R.id.pad).setPressed(false);
            findViewById(R.id.fire).setPressed(false);
            
            break;

        case MotionEvent.ACTION_MOVE:
            // �ȵ����¼����������ԭ��
            event.offsetLocation(mOffsetX, mOffsetY);
            
            ButtonType  type = GetButton(event.getX(actionIndex), event.getY(actionIndex));
            switch (type) {
            case UP:
            case DOWN:
            case LEFT:
            case RIGHT:
                UserAction  act = new UserAction(UserActionType.ACTION_MOVE);
                act.mValue = ButtonType.Convert2Dir(type).ordinal();
                thread.PushAction(act);
                break;
                
            case FIRE:
                act = new UserAction(UserActionType.ACTION_FIRE);
                thread.PushAction(act);
                break;

            default:    
                pMe.SetDir(Dir.NONE);
                if (BattleCity.smGameMode != GameMode.SINGLE) {
                    pMe.SendMoveMsg(Dir.NONE);    
                }
                break;
            }

            break;
            
        case MotionEvent.ACTION_DOWN:
            view.setPressed(true);
            
            switch (viewId) {
            case R.id.fire:
                mOffsetX = mFireRect.left;
                mOffsetY = mFireRect.top;
                
                UserAction  act = new UserAction(UserActionType.ACTION_FIRE);
                thread.PushAction(act);
                break;

            case R.id.pad:
                mOffsetX = mPadRect.left;
                mOffsetY = mPadRect.top;
    
                type = GetButton(event.getRawX(), event.getRawY());
                switch (type) {
                case UP:
                case DOWN:
                case LEFT:
                case RIGHT:
                    act = new UserAction(UserActionType.ACTION_MOVE);
                    act.mValue = ButtonType.Convert2Dir(type).ordinal();
                    thread.PushAction(act);
                    break;

                default:    
                    pMe.SetDir(Dir.NONE);
                    if (BattleCity.smGameMode != GameMode.SINGLE) {
                        pMe.SendMoveMsg(Dir.NONE);    
                    }
                    break;
                }
                
            default:
                break;
            }
            
            break;

        case MotionEvent.ACTION_POINTER_DOWN:   
            event.offsetLocation(mOffsetX, mOffsetY);
            type = GetButton(event.getX(actionIndex), event.getY(actionIndex));            
            switch (type) {
            case UP:
            case DOWN:
            case LEFT:
            case RIGHT:
                findViewById(R.id.pad).setPressed(true);
                
                UserAction  act = new UserAction(UserActionType.ACTION_MOVE);
                act.mValue = ButtonType.Convert2Dir(type).ordinal();
                thread.PushAction(act);
                break;

            case FIRE:
                findViewById(R.id.fire).setPressed(true);
                act = new UserAction(UserActionType.ACTION_FIRE);
                thread.PushAction(act);
                break;

            default:        
                break;
            }
            break;
            
        case MotionEvent.ACTION_POINTER_UP:
            event.offsetLocation(mOffsetX, mOffsetY);
            type = GetButton(event.getX(actionIndex), event.getY(actionIndex));
            
            switch (type) {
            case UP:
            case DOWN:
            case LEFT:
            case RIGHT:
                findViewById(R.id.pad).setPressed(false);
                UserAction act = new UserAction(UserActionType.ACTION_UP2);
                thread.PushAction(act);
                break;

            case FIRE:
                findViewById(R.id.fire).setPressed(false);
                break;
            
            default:
                break;
            }
            break;
            
        default:
            break;
        }

        return  isHandled;
    }
    
    @Override
    public void onBackPressed() {
        mInit = false;
        TimerManager.Instance().KillAll();
        Misc.UnInit();
        GameWorld.Instance().ResetPlayer();
        this.finish();
    }
}

class  UserAction {
    enum UserActionType {
        ACTION_UP,
        ACTION_MOVE,
        ACTION_FIRE,
        ACTION_UP2,
    };
    
    UserAction(UserActionType type) {
        mType = type;
        mValue= 0;
    }
    
    UserActionType  mType;
    int             mValue;
}
