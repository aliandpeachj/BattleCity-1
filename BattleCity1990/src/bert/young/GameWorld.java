package bert.young;

import java.io.InputStream;
import bert.young.Bluetooth.EnemyBornMsg;
import bert.young.Bluetooth.PlayerBornMsg;
import bert.young.Movable.Dir;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;


final class GameWorld {
    private static final String TAG = "GameWorld";

    /** ����̹��  */
    Tank   FindObject(int id) {
        if (id == PlayerTank.PLAYER1 && BattleCity.smGameMode == GameMode.CLIENT)
            return mPartner;

        if (id == PlayerTank.PLAYER2 && BattleCity.smGameMode == GameMode.SERVER)
            return mPartner;

        return mEnemyMgr.FindEnemy(id);
    }
    
    /** Forwarding function */
    void  SetFrozen(boolean frozen) {
        mEnemyMgr.SetFrozen(frozen);
    }
    
    /** ���ֹʱ���ã�����ֵ�ÿ��� */
    void  ResetPlayer() {
        mPlayer = null;
        mPartner= null;
    }

    /** Forwarding function */
    void ProtectHeadQuarters() {
        mMap.ProtectHeadQuarters();
    }
    
    
    private PlayerTank  mPlayer = null;
    private PlayerTank  mPartner = null; // ��һ��˫������

    public  PlayerTank  GetMe() {
        return  mPlayer;
    }
    
    public  PlayerTank  GetPartner() {
        return  mPartner;
    }
    public  void  SetPartner(PlayerTank tank) {
        mPartner = null;
    }
    /** ����PLAYER������Ϣ */
    void OnBornPlayer(PlayerBornMsg msg) {
        mPartner = new PlayerTank();
        mPartner.Init(BattleCity.smGameMode == GameMode.CLIENT);
        mPartner.SetPos(msg.mX * GameWorld.StandardUnit(), 
                         msg.mY * GameWorld.StandardUnit());
        mPartner.SetFaceDir(Dir.Convert2Dir(msg.mFace));
        mPartner.mID = msg.mID;
        
        Log.d(TAG, "Create partner " + mPartner.GetPos().x + ", " + mPartner.GetPos().y);
    }
    
    /** ����ENEMY������Ϣ */
    boolean OnBornEnemy(EnemyBornMsg msg) {
        int id   = msg.mID;
        int x    = msg.mX;
        int y    = msg.mY;
        int type = msg.mType;
    
        MyResource.Assert(mEnemyMgr != null, "Why enemy mgr is null");
        Log.d(TAG, "Create enemy id = " + id);
        return mEnemyMgr.OnBornEnemy(id, x * GameWorld.StandardUnit(),
                                   y * GameWorld.StandardUnit(), type);
    }  
    
    static int   STEP = 0;
    public static int StandardUnit() {
        return  STEP / 4;
    }

    private final Rect mSrcRect = new Rect();
    private final Rect mDstRect = new Rect();
    
    /** ��Ϸ���Ĵ�С ������������ */
    public  int GetSceneWidth() {
        return  mMap.GetSceneWidth();
    }
    
    public int GetTerrain(int gridx, int gridy, int tmask) {
        return mMap.GetTerrain(gridx, gridy, tmask);
    }
    public void ClearTerrain(int gridx, int gridy, int mask) {
        mMap.ClearTerrain(gridx, gridy, mask);
    }

    /** ͼƬ����ʾ�ߴ� */
    public int GetImgWidth() {
        return mMap.GetImgWidth();
    }
    
    /** ��ײ������ͼƬ��С��һ��*/
    public int GetGridWidth() {
        return  mMap.GetGridWidth();
    }
    
    /** Ψһ��Ϸ���� */
    private static GameWorld smInstance = new GameWorld();
    
    /** ȫ�ַ��ʵ� */
    public  static GameWorld Instance() {
        return  smInstance;
    }

    /** ��Ϸ״̬ */
    enum GameState
    {
        NONE,
        BEGINSTAGE,
        PLAYING,
        PAUSED,
        ENDSTAGE,   // ˲ʱ״̬�����ú�����
        ENDSTAGING, // ��״̬�������룬��ͨ�ػ���
        LOSE,

        MAXSTATE,
    };
    
    private GameState    mState = GameState.NONE;   
    private Headquarters mHead  = new Headquarters();
    public  boolean  HitHeadquarters(Movable pOther) {
        return mHead.Intersect(pOther);
    }
    public void  DestroyHeadquarters() {
        if (ObjectState.IsAlive(mHead.mState))
            mHead.SetState(ObjectState.EXPLODE1);
    }
    public boolean IsHeadquartersOk() {
        return ObjectState.NORMAL == mHead.GetState();
    }
    
    private  Bonus mBonus = null;
    public   void  SetBonus(Bonus  bonus) {
        mBonus = bonus;
    }
    
    /** ��Ϸ��ǰѭ������ */
    private int  mLoopCnt = 0;
    public  int GetLoopCnt() {
        return   mLoopCnt;
    }
    
    /** �߼�֡�� */
    public static final int FPS = 18;
    
    /** ��Ϸ��ʼʱ�� */
    private long mStartTime;    // Ӧ������Ϸ��ʼʱ����
    
    /** ��Ϸ�жϵ�ʱ�� */
    //private long m_pauseTime = 0;
    
    /** ��ǰ�ؿ��� */
    private int mStage      = 1;
    private static final int MAX_STAGE = 35;

    /** ���� */
    private Paint mPaint = new Paint();
    /** �߽续�� */
    private Paint mBorderPaint = new Paint();
    
    /** �л�״̬ */
    public void SetState(GameState newState) {
        mState = newState;
    }
    
    boolean IsPlaying() {
        return mState == GameState.PLAYING ||
               mState == GameState.PAUSED;
    }
 
    private Map  mMap;
    /** ��ʼ�� */
    public boolean Init() {
        GameView  view = GameView.Instance();
        if (view.getWidth() != view.getHeight()) {
            Log.e(TAG, "You must assure view is square, width == height");
            return false;
        }

        mMap  = new Map();
        mMap.Init(view.getWidth());

        mStage = Misc.GetDefaultStage();
        if (mStage < 1)    mStage = 1;
        else if (mStage > MAX_STAGE)    mStage = MAX_STAGE;
        
        if (BattleCity.smGameMode != GameMode.SINGLE)
            mStage = 1;
        
        // !!!�����ƶ��ĵ�λ,Ҳ��̹������Ƕ�������(���ɴ�)
        STEP =  GetGridWidth() / 2;
        Log.d(TAG, "m_imgWidth = " + GetImgWidth());
        Log.d(TAG, "Grid size = " + GetGridWidth());
        Log.d(TAG, "STEP  = " + STEP);
        MyResource.Assert(STEP % 4 == 0, "Step must be 4s, wrong step = " + STEP);
        
        SetState(GameState.NONE);

        mPaint.setAntiAlias(true);
        mBorderPaint.setAntiAlias(true);
        mBorderPaint.setAlpha(200);
        mBorderPaint.setStrokeWidth(4);
        mBorderPaint.setColor(Color.DKGRAY);

        // ��Դ��ʼ��
        if (!MyResource.Init(view.getResources())) {
            Log.e(TAG, "Can not init resources");
            return false;
        }

        return true;
    }

    EnemyManager   mEnemyMgr;
    
    /** ��Ϸ���� */
    void   GameOver() {
        Misc.PlaySound(AudioID.AUDIO_OVER);
        SetState(GameState.LOSE);
        
        TimerManager.Instance().AddTimer(new TimerManager.Timer(2500, 1){
            @Override
            boolean _OnTimer() {   // �ص�������
                GameActivity.Instance().finish();
                return  false;
            }
        });
    }
    
    /** ÿһ��Ҫ�ܵ��Է���READY�����ܿ�ʼ*/
    public  void  StartPlay() {
        mLoopCnt   = 0;
        mStartTime = System.currentTimeMillis();
        mBonus = null;
        GameView.Instance().GetThread().ClearCmd();
        Misc.PlaySound(AudioID.AUDIO_OPENING);
        InputStream inFile = GameView.Instance().getResources().openRawResource(R.raw.stage01 + mStage - 1); 
        if (!mMap.LoadMap(inFile)) {
            Log.d(TAG, "Failed loadmap");
            MyResource.Assert(false, "Load map failed, Stage = " + mStage);  
        }
    
        mEnemyMgr = new EnemyManager();
        mEnemyMgr.Init(mStage); 
        SetFrozen(false);
        SetState(GameState.PLAYING);
        Log.d(TAG, "On Timer, start playing");
    }
    
    /** �����Ƿ�ִ���߼�֡ */
    public boolean ShouldUpdate(final long  now) {
        switch (mState) {
        case NONE:
            Log.d(TAG, "State none");
            SetState(GameState.BEGINSTAGE);
            mHead.Init();

            //�µ�һ�أ�Ӧ��ɾ�����е�TIMERS
            TimerManager.Instance().KillAll();
            mMap.ClearGrassInfo();
 
            // ����2�� ������Ϸ����
            TimerManager.Instance().AddTimer(new TimerManager.Timer(1500, 1) {
                @Override
                boolean _OnTimer() {
                    if (null == mPlayer)
                        mPlayer = new PlayerTank();
            
                    GameMode mode = BattleCity.smGameMode;
                    if (!mPlayer.IsAlive()) {
                        mPlayer.Init(mode == GameMode.CLIENT);
                    } else {
                        mPlayer.Reset(mode == GameMode.CLIENT);
                    }
                
                    if (BattleCity.smGameMode == GameMode.SINGLE) {
                        StartPlay();
                    }
                    else {
                        Log.d(TAG, "Game mode " + BattleCity.smGameMode);
                        mPlayer.SendBornMsg();
                        // ���ڷ�������ѯ�� ARE YOU READY
                        // ���ڿͻ��ˣ��ȴ�������ѯ�ʣ������� I AM READY��START_PLAY()
                        // �������յ�I AM READY��START_PLAY
                        if (BattleCity.smGameMode == GameMode.SERVER) {
                            Bluetooth.ServerReadyMsg msg = Bluetooth.Instance().new ServerReadyMsg();
                            Bluetooth.Instance().SendMessage(msg);
                            Log.d(TAG, "send server ready");
                        }
                    }

                    return false;
                }
            });

            return false;
            
        case BEGINSTAGE:
            return false;
            
        case PLAYING:
            return (now - mStartTime) * FPS > mLoopCnt * 1000;
            
        case PAUSED:
            // ��PAUSING 
            break;
            
        case ENDSTAGE:
            // ����2�� ������Ϸ����
            TimerManager.Instance().AddTimer(new TimerManager.Timer(1500, 1) {
                @Override
                protected boolean _OnTimer() {
                    if (++ mStage > MAX_STAGE)
                        mStage = 1;

                    GameWorld.this.SetState(GameState.NONE);
                    Log.d(TAG, "On Timer, End stage over");
                    
                    // �ر��������ţ��µ�һ�ؿ�ʼ��
                    AudioPool.Stop();
                    return   false;
                }
            });
            
            SetState(GameState.ENDSTAGING);

            break;
            
        case ENDSTAGING:
            break;
            
        case LOSE:
            // ��GAMEOVER��������Ϸ
            break;
            
        default:
            break;
        }
        
        return false;
    }

    public   int  GetRemainEnemy() {
        if (null == mEnemyMgr)
            return 0;

        return  mEnemyMgr.GetRemainEnemy();
    }

    /** ��Ϸ�߼�ѭ�� */
    public void UpdateWorld() {
        switch (mState) {
        case PLAYING:
            ++ mLoopCnt;
            //  ֻ��4�������ܳ���
            if (mEnemyMgr.NoEnemy()) {
                // ������
                mState  = GameState.ENDSTAGE;
                break;
            }
            if (mEnemyMgr.CanBornEnemy(mLoopCnt)) {
                EnemyTank   curEnemy = mEnemyMgr.BornEnemy(mLoopCnt);
                MyResource.Assert(curEnemy != null, "Can not born enemy");
                
                switch (mEnemyMgr.GetBornPos()) {
                case LEFT:
                    curEnemy.SetPos(0, 0);
                    break;

                case CENTER:
                    curEnemy.SetPos((GameView.Instance().getWidth() - curEnemy.GetBodySize()) / 2, 0);
                    break;

                case RIGHT:
                    curEnemy.SetPos(GameView.Instance().getWidth() - curEnemy.GetBodySize(), 0);
                    break;

                default:
                    MyResource.Assert(false,  "Wrong born position");
                    break;
                }

                // ˢ�µ�����Ŀ��Ϣ
                InfoView.Instance().postInvalidate();
                if (BattleCity.smGameMode == GameMode.SERVER)
                    curEnemy.SendBornMsg();
            }
            
            // ��Ҹ���
            if (null != mPlayer) {
                mPlayer.UpdateBullets();
                mPlayer.Update();
            }
            
            if (null != mPartner) {
                mPartner.UpdateBullets();
                mPartner.Update();
            }
        
            // ���˸���
            mEnemyMgr.Update();
            
            // BONUS����
            if (mBonus != null) {
                mBonus.Update();
            }
            
            // �ܲ�����
            mHead.Update();
        }
    }
    
    /** ������Ϸ���� */
    public void PaintWorld(Canvas   canvas) {
        // �����������
        canvas.drawColor(Color.BLACK);

        switch (mState)  {
        case NONE:
            return;

        case BEGINSTAGE:
            canvas.drawColor(Color.GRAY);
            Paint  paint = new Paint();
            paint.setAntiAlias(true);
            paint.setColor(Color.BLACK);
            paint.setTextSize(12 * StandardUnit());
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("STAGE " + (mStage), canvas.getHeight() / 2,
                    canvas.getWidth() / 2, paint);
            break;
            
        case PLAYING:
            //1 �Ȼ����Σ�����Ҫ��ס�ݵ����꣬��Ϊ�ݵ�Ҫ����̹������
            //2 ��̹�˺��ӵ���
            //3 ���ݵ�
            //4 ��BONUS
            final int rawImgSize = MyResource.RawTankSize();
            final int tankImgType = 14; // ��14��̹��ͼƬ��ǰ��
            
            // 1 ������
            final Rect src = mSrcRect;
            final Rect dst = mDstRect;
            for (int i = 0; i < Map.GRID_CNT; ++ i) {
                for (int j = 0; j < Map.GRID_CNT; ++ j) {
                    final char info = mMap.GetGridInfo(i, j);
                    final int terrainType = (info & Map.TERRAIN_TYPE);
                    
                    if (terrainType == Map.EAGLE ||
                        terrainType == Map.NONE ||
                        terrainType == Map.GRASS)
                        continue;

                    // ���ﲻҪ���ݵ�
                    if (Map.BLOCK != terrainType) { // ֻ��ש����Ҫ��һ��ͼϸ�ֳ� 4 * 4
                        // ����������˵��GRID�͹���
                        src.left = (tankImgType + terrainType) * rawImgSize;
                        src.top = MyResource.RawTankSize();
                        src.right = src.left + rawImgSize / 2;
                        src.bottom = src.top + rawImgSize / 2;

                        dst.left = j * GetGridWidth();
                        dst.top = i * GetGridWidth();
                        dst.right = dst.left + GetGridWidth();
                        dst.bottom = dst.top + GetGridWidth();
                        
                        // ��ʱ��һ��ˮ����˸,Ч����Ȼ�ܺ�
                        if (Map.WATER == terrainType &&
                            (mLoopCnt / 5) % 2 == 0) {
                            src.left = (tankImgType + 0) * rawImgSize;
                            src.right = src.left + rawImgSize / 2;
                        }

                        canvas.drawBitmap(MyResource.GetSpirit(), src, dst,
                                mPaint);
                        continue;
                    }

                    // ��˳��ש���4������
                    if (0 != (info & Map.TERRAIN_TYPE_LEFTTOP)) {
                        src.left = (tankImgType + terrainType) * rawImgSize;
                        src.top = MyResource.RawTankSize();
                        src.right = src.left + rawImgSize / 4;
                        src.bottom = src.top + rawImgSize / 4;

                        dst.left = j * GetGridWidth();
                        dst.top = i * GetGridWidth();
                        dst.right = dst.left + GetGridWidth() / 2;
                        dst.bottom = dst.top + GetGridWidth() / 2;

                        canvas.drawBitmap(MyResource.GetSpirit(), src, dst,
                                mPaint);
                    }
                    if (0 != (info & Map.TERRAIN_TYPE_RIGHTTOP)) {
                        src.left = (tankImgType + terrainType) * rawImgSize
                                + rawImgSize / 4;
                        src.top = MyResource.RawTankSize();
                        src.right = src.left + rawImgSize / 4;
                        src.bottom = src.top + rawImgSize / 4;

                        dst.left = j * GetGridWidth() + GetGridWidth() / 2;
                        dst.top = i * GetGridWidth();
                        dst.right = dst.left + GetGridWidth() / 2;
                        dst.bottom = dst.top + GetGridWidth() / 2;

                        canvas.drawBitmap(MyResource.GetSpirit(), src, dst,
                                mPaint);
                    }
                    if (0 != (info & Map.TERRAIN_TYPE_LEFTBOTTOM)) {
                        src.left = (tankImgType + terrainType) * rawImgSize;
                        src.top = MyResource.RawTankSize() + rawImgSize / 4;
                        src.right = src.left + rawImgSize / 4;
                        src.bottom = src.top + rawImgSize / 4;

                        dst.left = j * GetGridWidth();
                        dst.top = i * GetGridWidth() + GetGridWidth() / 2;
                        dst.right = dst.left + GetGridWidth() / 2;
                        dst.bottom = dst.top + GetGridWidth() / 2;

                        canvas.drawBitmap(MyResource.GetSpirit(), src, dst,
                                mPaint);
                    }
                    if (0 != (info & Map.TERRAIN_TYPE_RIGHTBOTTOM)) {
                        src.left = (tankImgType + terrainType) * rawImgSize
                                + rawImgSize / 4;
                        src.top = MyResource.RawTankSize() + rawImgSize / 4;
                        src.right = src.left + rawImgSize / 4;
                        src.bottom = src.top + rawImgSize / 4;

                        dst.left = j * GetGridWidth() + GetGridWidth() / 2;
                        dst.top = i * GetGridWidth() + GetGridWidth() / 2;
                        dst.right = dst.left + GetGridWidth() / 2;
                        dst.bottom = dst.top + GetGridWidth() / 2;

                        canvas.drawBitmap(MyResource.GetSpirit(), src, dst,
                                mPaint);
                    }
                } // end for j
            } // end for i
            
            // ���ܲ�
            mHead.Paint(canvas);
            
            // 2 ��̹���Լ��ӵ�
            if (null != mPlayer) {
                mPlayer.Paint(canvas);
                mPlayer.RenderBullets(canvas);
            }
            
            if (null != mPartner) {
                mPartner.Paint(canvas);
                mPartner.RenderBullets(canvas);
            }

            // 3 ������
            mEnemyMgr.Render(canvas);

            // ���ݵ�
            for (Point pos : mMap.GetGrassInfo()) {
                src.left = (tankImgType + Map.GRASS) * rawImgSize;
                src.top  = MyResource.RawTankSize();
                src.right  = src.left + rawImgSize / 2;
                src.bottom = src.top + rawImgSize / 2;
        
                dst.left = pos.y * GetGridWidth();
                dst.top  = pos.x * GetGridWidth();
                dst.right = dst.left + GetGridWidth() ;
                dst.bottom = dst.top + GetGridWidth();

                canvas.drawBitmap(MyResource.GetSpirit(), src, dst, mPaint);
            }
            
            // ��BONUS
            if (mBonus != null) {
                mBonus.Paint(canvas);
            }

            // ���߽�
            canvas.drawLine(0, 0, GetSceneWidth(), 0, mBorderPaint);
            canvas.drawLine(0, 0, 0, GetSceneWidth(), mBorderPaint);
            canvas.drawLine(GetSceneWidth(), GetSceneWidth(), 0, GetSceneWidth(), mBorderPaint);
            canvas.drawLine(GetSceneWidth(), GetSceneWidth(), GetSceneWidth(), 0, mBorderPaint);
            // fall through

        case PAUSED:
            // if (paused)��PAUSING ����
            break;
            
        case ENDSTAGING:
            // ����������ϲ����
            mDstRect.left = 0;
            mDstRect.top  = 0;
            mDstRect.right = mDstRect.left + GetSceneWidth();
            mDstRect.bottom = mDstRect.top + GetSceneWidth();
            canvas.drawBitmap(MyResource.GetEndStage(), null, mDstRect, mPaint);

            paint = new Paint();
            paint.setAntiAlias(true);
            paint.setColor(Color.WHITE);
            paint.setTextSize(12 * StandardUnit());
            paint.setTextAlign(Paint.Align.CENTER);

            canvas.drawText("STAGE " + mStage, canvas.getHeight() / 2,
                    40 * StandardUnit(), paint);
            break;
            
        case LOSE:
            // ��GAMEOVER��������Ϸ
            mDstRect.left = GetSceneWidth() / 2 - MyResource.GetOver().getWidth() / 2;
            mDstRect.top  = GetSceneWidth() / 2 - MyResource.GetOver().getHeight() / 2;
            mDstRect.right = mDstRect.left +  MyResource.GetOver().getWidth() ;
            mDstRect.bottom = mDstRect.top +  MyResource.GetOver().getHeight() ;

            canvas.drawBitmap(MyResource.GetOver(), null, mDstRect, mPaint);
            
            // ���߽�
            canvas.drawLine(0, 0, GetSceneWidth(), 0, mBorderPaint);
            canvas.drawLine(0, 0, 0, GetSceneWidth(), mBorderPaint);
            canvas.drawLine(GetSceneWidth(), GetSceneWidth(), 0, GetSceneWidth(), mBorderPaint);
            canvas.drawLine(GetSceneWidth(), GetSceneWidth(), GetSceneWidth(), 0, mBorderPaint);
            
            break;
            
        default:
            break;
        }
    }
    
    /** �볡����̬�������ײ��� */
    boolean HitTest(Movable  pThis) {
        // �ȳ��������е���̹���Լ��ӵ���ײ
        if (mEnemyMgr.HitTest(pThis))
            return true;

        // �������������
        if (null != mPlayer && mPlayer.HitTestBullets(pThis))
            return true;
        
        if (null != mPartner && mPartner.HitTestBullets(pThis))
            return true;
        
        if (null != mPlayer && mPlayer.IsAlive()) {
            if (pThis.HitTest(mPlayer))
                return true;
        }
        
        if (null != mPartner && mPartner.IsAlive()) {
            if (pThis.HitTest(mPartner))
                return true;
        }

        return false;
    }
}
