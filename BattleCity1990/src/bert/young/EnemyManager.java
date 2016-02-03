package bert.young;

import java.util.Random;
import android.graphics.Canvas;
import android.util.Log;
import bert.young.Tank.TankType;

final class EnemyManager {
    /** ����������Ϣ */
    boolean OnBornEnemy(int id, int x, int y, int type) {
        EnemyTank   enemy = _GetEnemy();
        if (enemy == null)  {
            Log.e(Bullet.TAG, "Error OnBornEnemy type " + type + ", mID " + id);
            return false;
        }

        Log.d(Bullet.TAG, "OnBornEnemy type " + type + ", mID " + id);
        TankType  enumType = TankType.values()[type];

        enemy.Init(enumType);
        enemy.SetPos(x, y);
        enemy.mID = id;
        MyResource.Assert(id > 0, "ID must be greater than zero");
        
        InfoView.Instance().postInvalidate();
        ++ mCurEnemyIdx;
        return true;
    }
    
    EnemyManager() {
    }
    
    /** ����id���ҵ��� */
    EnemyTank FindEnemy(int id) {
        for (EnemyTank enemy : mEnemyTanks) {
            if (null != enemy && enemy.mID == id)
                return enemy;
        }
        
        return null;
    }

    // ��ʼ����������
    void Init(int dummyStage) {
        m_enemyBornTimer = 0;
        mCurEnemyIdx  = 0;
        mBornPos        = BornPos.LEFT;
        
        if (BattleCity.smGameMode == GameMode.SINGLE)
            mEnemyTanks = new EnemyTank[4];
        else
            mEnemyTanks = new EnemyTank[6]; 
        
        for (int i = 0; i < mEnemyTanks.length; ++ i) {
            mEnemyTanks[i] = new EnemyTank();
        }
    
        // ÿ��3����̹�ˣ���ʼ���0����һ����1-3�У��ڶ�����8-11����������15-18
        Random  rand = new Random(System.currentTimeMillis());
        
        final int firstRed =  1 + rand.nextInt(3);
        final int secRed   =  8 + rand.nextInt(4);
        final int thirdRed = 15 + rand.nextInt(4);
        
        for (int i = 0; i < mEnemyTypes.length; ++ i) {
            switch (rand.nextInt(Tank.TankType.MAX.ordinal()) / 2) {
            case 0:
                mEnemyTypes[i] = Tank.TankType.NORMAL;
                break;

            case 1:
                mEnemyTypes[i] = Tank.TankType.FAST;
                break;
                
            case 2:
                mEnemyTypes[i] = Tank.TankType.SMART;
                break;

            case 3:
                mEnemyTypes[i] = Tank.TankType.STRONG;
                break;
                
            default:
                MyResource.Assert(false, "ERROR ENEMY TYPE");
                break;
            }
        }
        
        mEnemyTypes[firstRed] = TankType.values()[mEnemyTypes[firstRed].ordinal() + 1];
        mEnemyTypes[secRed]   = TankType.values()[mEnemyTypes[secRed].ordinal()   + 1];
        mEnemyTypes[thirdRed] = TankType.values()[mEnemyTypes[thirdRed].ordinal() + 1];
    }

    /** ��ȡʣ�������Ŀ*/
    int  GetRemainEnemy() {
        return   MAX_ENEMY - mCurEnemyIdx;
    }
    
    /** ��ȡ��ͼ�ϵ�����Ŀ*/
    int  GetCurrentEnemyNum() {
        int num = 0;
        for (EnemyTank enemy : mEnemyTanks) {
            if (null != enemy && enemy.IsValid())
                ++ num;
        }
        
        return num;
    }
    
    /** �Ƿ��е���*/
    boolean  NoEnemy() {
        return  mCurEnemyIdx >= MAX_ENEMY && 0 == GetCurrentEnemyNum();
    }
    
    /** �Ƿ���Գ���*/
    boolean CanBornEnemy(int currentLoop) {
        if (BattleCity.smGameMode == GameMode.CLIENT)
            return false;
        
        if (mCurEnemyIdx >= MAX_ENEMY)
            return false;
        
        if (0 == GetCurrentEnemyNum())
            return true;
        
        if (GetCurrentEnemyNum() < mEnemyTanks.length &&
            currentLoop >= m_enemyBornTimer + BORN_INTERVAL) {
            return  true;
        }
        
        return false;    
    }
    
    /** ��������*/
    EnemyTank BornEnemy(int currentLoop) {
        EnemyTank enemy = _GetEnemy();

        enemy.Init(mEnemyTypes[mCurEnemyIdx]);
        
        ++ mCurEnemyIdx;
        m_enemyBornTimer = currentLoop;
        
        return  enemy;
    }
    
    private EnemyTank _GetEnemy() {
        EnemyTank   result = null;

        for (EnemyTank enemy : mEnemyTanks) {
            if (!enemy.IsValid())
                result = enemy;
        }
        
        if (result == null && BattleCity.smGameMode == GameMode.CLIENT) {
        	 for (EnemyTank enemy : mEnemyTanks) {
        		 Log.e(Bullet.TAG, "enemy state " + enemy.mState);
             }
        }

        return result;
    }
    
    /** ��ȡ������*/
    BornPos  GetBornPos() {
        BornPos  result = mBornPos;
        switch (mBornPos) {
        case LEFT:
            mBornPos = BornPos.CENTER;
            break;

        case CENTER:
            mBornPos = BornPos.RIGHT;
            break;

        case RIGHT:
            mBornPos = BornPos.LEFT;
            break;

        default:
            MyResource.Assert(false,  "Wrong born position");
            break;
        }

        return  result;
    }

    /** ��Ϸ�߼�����*/
    public void Update() {
        for (EnemyTank enemy : mEnemyTanks) {
            enemy.UpdateBullets();
            if (!mEnemyFrozen)
                enemy.Update();
        }
    }
    
    /** ������*/
    public void Render(Canvas  canvas) {
        for (EnemyTank enemy : mEnemyTanks) {
            if (null != enemy) {
                enemy.Paint(canvas);
                enemy.RenderBullets(canvas);
            }
        }    
    }
    
    /** ������ײ����*/
    public boolean HitTest(Movable  pThis) {
        for (EnemyTank enemy : mEnemyTanks) {
            if (enemy.HitTestBullets(pThis)) {
                // ���ﲻ����enemy״̬����Ϊ�����ˣ������ӵ���Ȼ�����ٷ�һ��
                return true;
            }
            
            if (enemy.IsAlive() && !enemy.IsGod()) {
                if (pThis.HitTest(enemy))
                    return true;
            }
        }
        
        return  false;
    }
    
    /** ը�����л�ĵ���*/
    boolean  BombAll() {
        boolean  hasEnemy = false;
        for (EnemyTank enemy : mEnemyTanks) {
            if (null != enemy &&
                enemy.IsValid()) {
                hasEnemy  = true;
                enemy.mHP = 0;
                enemy.SetState(ObjectState.EXPLODE1);
                if (enemy.ShouldSync())
                    enemy.SendHurtMsg();
            }
        }
        
        return hasEnemy;
    }
    
    /** ����/�ⶳ���е��� */
    void  SetFrozen(boolean frozen) {
        mEnemyFrozen = frozen;
        TimerManager.Instance().KillTimer(mFrozenTimer);

        if (frozen) {    // ����14��
            mFrozenTimer.SetRemainCnt(1);
            TimerManager.Instance().AddTimer(mFrozenTimer);
        }
    }

    private static final int BORN_INTERVAL = 3 * GameWorld.FPS / 2;
    private static final int MAX_ENEMY     = 20;
    
    private  Tank.TankType[]   mEnemyTypes = new Tank.TankType[MAX_ENEMY];
    private  EnemyTank []      mEnemyTanks = null;    
    private  int  m_enemyBornTimer; // ���˳�����һ����ʱ����
    private  int  mCurEnemyIdx ; // ��0-19��
    
    enum BornPos {
        LEFT,
        CENTER,
        RIGHT,
    }
    private  BornPos  mBornPos;
    private  boolean  mEnemyFrozen;
    private  final TimerManager.Timer  mFrozenTimer = new TimerManager.Timer(14 * 1000, 1) {
        @Override
        boolean _OnTimer() {
            EnemyManager.this.SetFrozen(false);
            return  false;
        }
    };
}
