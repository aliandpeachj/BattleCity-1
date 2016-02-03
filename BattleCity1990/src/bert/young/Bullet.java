package bert.young;

import bert.young.BulletManager.BulletPower;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;

final class Bullet extends Movable {
    public  static final int   BULLET_SPEED = 7 * GameWorld.StandardUnit();
    public  static final int   FAST_SPEED   = 10 * GameWorld.StandardUnit();
    public  static final String TAG = "Bullet";

    /** ������ */
    int GetBodySize() {
        return GameWorld.Instance().GetGridWidth() / 2;
    }

    /** Init owner and state */
    boolean Init(Tank pOwner) {
        if (null == pOwner)
            return  false;
        
        mOwner = pOwner;
        mState = ObjectState.NORMAL;
        mHP    = 1;

        return true;
    }
    
    /** ֱ����ʧ */
    void _Vanish() {
        this.SetState(ObjectState.INVALID);
    }

    /** */
    void    Paint(Canvas canvas) {
        if (!ObjectState.IsValid(mState))
            return;
        
        final int offsetPixelX = 29 * MyResource.RawTankSize();

        switch (GetState())
        {
        case EXPLODE1:
        case EXPLODE2:
        case EXPLODE3:
        case EXPLODE4:
            switch (mFaceDir) {
            case UP:
            case DOWN:
            case LEFT:
            case RIGHT:
                final int rawExplodeSize = MyResource.RawExplodeSize();
                final int explodeSize    = mOwner.GetBodySize();
                mDstRect.left   = mPos.x + GetBodySize() / 2 - explodeSize / 2;
                mDstRect.top    = mPos.y + GetBodySize() / 2 - explodeSize / 2;
                mDstRect.right  = mDstRect.left + explodeSize ;
                mDstRect.bottom = mDstRect.top + explodeSize;

                mSrcRect.left   = offsetPixelX + (GetState().ordinal() - ObjectState.EXPLODE1.ordinal()) * rawExplodeSize;
                mSrcRect.top    = 0;
                mSrcRect.right  = mSrcRect.left + rawExplodeSize;
                mSrcRect.bottom = mSrcRect.top  + rawExplodeSize;
    
                canvas.drawBitmap(MyResource.GetSpirit(), mSrcRect, mDstRect, mPaint);
                break;

            default:
                MyResource.Assert(false, "bullet must have dir!");
                break;
            }
            break;

        default:
            int  imgSeq = 0;
            switch (mFaceDir)  {
            case UP:
                imgSeq = 0;
                break;
            case DOWN:
                imgSeq = 2;
                break;
            case LEFT:
                imgSeq = 3;
                break;
            case RIGHT:
                imgSeq = 1;
                break;
            default:
                MyResource.Assert(false, "Render bullet with no direction!");
                break;
            }
            
            final int size = MyResource.RawBulletSize();
            mSrcRect.left   = imgSeq * size;
            mSrcRect.top    = 0;
            mSrcRect.right  = mSrcRect.left + size;
            mSrcRect.bottom = mSrcRect.top  + size;
            canvas.drawBitmap(MyResource.GetBulletBmp(), mSrcRect, GetBodyRect(), mPaint);
            break;
        }
    }
    
    void    Update() {
        if (IsAlive()) {
            UpdateMove();
        }

        switch (GetState()) {
        case EXPLODE1:
        case EXPLODE2:
        case EXPLODE3:
            if (mStateChanged) {
                mStateChanged = false;
                TimerManager.Instance().AddTimer(new TimerManager.Timer(70, 1) {
                    @Override
                    boolean _OnTimer() {
                        ObjectState  state = Bullet.this.mState;
                        if (state == ObjectState.EXPLODE1 ||
                            state == ObjectState.EXPLODE2 ||
                            state == ObjectState.EXPLODE3)
                            Bullet.this.SetState(ObjectState.values()[state.ordinal() + 1]);
                      
                        return false;
                    }
                });
            }
            break;

        case EXPLODE4:
            if (mStateChanged) {
                mStateChanged = false;

                TimerManager.Instance().AddTimer(new TimerManager.Timer(70, 1) {
                    @Override
                    boolean _OnTimer() {
                        Bullet.this.SetState(ObjectState.INVALID);
                        return false;
                    }
                });
            }
            break;

        default:
            break;
        }
    }

    /** ���� */
    void    OnHurt() {
        mHP  = 0;
        SetState(ObjectState.EXPLODE1);
    }
    
    /** */
    ObjectType GetType() {
        return ObjectType.BULLET; 
    }
    
    /** ��⾲̬�����赲 */
    boolean CheckTerrain(Pos pos) {
        GameWorld rWorld = GameWorld.Instance();
        if (pos.x < 0 ||
            pos.y < 0 ||
            pos.x >= rWorld.GetSceneWidth() ||
            pos.y >= rWorld.GetSceneWidth()) {
            return false;    
        }
        
        int destGridY = pos.x / rWorld.GetGridWidth(),
            destGridX = pos.y / rWorld.GetGridWidth();

        int destGridyRight = (pos.x + GetBodySize()) / rWorld.GetGridWidth(),
            destGridxRight = destGridX;

        int destGridyBottom = destGridY,
            destGridxBottom = (pos.y + GetBodySize()) / rWorld.GetGridWidth();

        // !! ����target���꣬�ж��ڸ��ӵ��������ĸ�λ��
        boolean bLeft   = false;
        boolean bTop    = false;
        if (pos.x % rWorld.GetGridWidth() < rWorld.GetGridWidth() / 2)
            bLeft = true;
        if (pos.y % rWorld.GetGridWidth() < rWorld.GetGridWidth() / 2)
            bTop = true;

        // һ���ӵ��ض���λ������GRID�ķָ���֮�ϣ�
        int     type1 = Map.INVALID,
                type2 = Map.INVALID;
        int     anotherDestX = -1,
                anotherDestY = -1;

        switch (mDir) {
        // �����ǹ��ڴ�ש������⴦��
        case UP:
        case DOWN:
            anotherDestX = destGridxRight;
            anotherDestY = destGridyRight;
            if (bTop) {
                type1 = rWorld.GetTerrain(destGridX, destGridY, Map.TERRAIN_TYPE_RIGHTTOP);
                type2 = rWorld.GetTerrain(anotherDestX, anotherDestY, Map.TERRAIN_TYPE_LEFTTOP);
            } else {
                type1 = rWorld.GetTerrain(destGridX, destGridY, Map.TERRAIN_TYPE_RIGHTBOTTOM);
                type2 = rWorld.GetTerrain(anotherDestX, anotherDestY, Map.TERRAIN_TYPE_LEFTBOTTOM);
            }

            if (Map.BLOCK != type1 && Map.BLOCK != type2)
                break;

            // at least one has block
            if (Map.BLOCK == type1) {
                if (bTop) {
                    rWorld.ClearTerrain(destGridX, destGridY, Map.TERRAIN_TYPE_RIGHTTOP);
                    rWorld.ClearTerrain(destGridX, destGridY, Map.TERRAIN_TYPE_LEFTTOP);// ������
                } else {
                    rWorld.ClearTerrain(destGridX, destGridY, Map.TERRAIN_TYPE_RIGHTBOTTOM);
                    rWorld.ClearTerrain(destGridX, destGridY, Map.TERRAIN_TYPE_LEFTBOTTOM);
                }
            }

            if (Map.BLOCK == type2) {
                if (bTop) {
                    rWorld.ClearTerrain(anotherDestX, anotherDestY, Map.TERRAIN_TYPE_RIGHTTOP);
                    rWorld.ClearTerrain(anotherDestX, anotherDestY, Map.TERRAIN_TYPE_LEFTTOP);
                } else {
                    rWorld.ClearTerrain(anotherDestX, anotherDestY, Map.TERRAIN_TYPE_RIGHTBOTTOM);
                    rWorld.ClearTerrain(anotherDestX, anotherDestY, Map.TERRAIN_TYPE_LEFTBOTTOM);
                }
            }
            
            mPos.x = pos.x;
            mPos.y = pos.y; // ��������
            return   false;

        case LEFT:
        case RIGHT:
            anotherDestX = destGridxBottom;
            anotherDestY = destGridyBottom;
            
            if (bLeft) {
                type1 = rWorld.GetTerrain(destGridX, destGridY, Map.TERRAIN_TYPE_LEFTBOTTOM);
                type2 = rWorld.GetTerrain(anotherDestX, anotherDestY, Map.TERRAIN_TYPE_LEFTTOP);
            } else {
                type1 = rWorld.GetTerrain(destGridX, destGridY, Map.TERRAIN_TYPE_RIGHTBOTTOM);
                type2 = rWorld.GetTerrain(anotherDestX, anotherDestY, Map.TERRAIN_TYPE_RIGHTTOP);
            }
            
            if (Map.BLOCK != type1 && Map.BLOCK != type2)
                break;

            // at least one has block
            if (Map.BLOCK == type1) {
                if (bLeft) {
                    rWorld.ClearTerrain(destGridX, destGridY, Map.TERRAIN_TYPE_LEFTBOTTOM);
                    rWorld.ClearTerrain(destGridX, destGridY, Map.TERRAIN_TYPE_LEFTTOP);
                } else {
                    rWorld.ClearTerrain(destGridX, destGridY, Map.TERRAIN_TYPE_RIGHTBOTTOM);
                    rWorld.ClearTerrain(destGridX, destGridY, Map.TERRAIN_TYPE_RIGHTTOP);
                }
            }

            if (Map.BLOCK  == type2) {
                if (bLeft) {
                    rWorld.ClearTerrain(anotherDestX, anotherDestY, Map.TERRAIN_TYPE_LEFTBOTTOM);
                    rWorld.ClearTerrain(anotherDestX, anotherDestY, Map.TERRAIN_TYPE_LEFTTOP);
                }    else {
                    rWorld.ClearTerrain(anotherDestX, anotherDestY, Map.TERRAIN_TYPE_RIGHTBOTTOM);
                    rWorld.ClearTerrain(anotherDestX, anotherDestY, Map.TERRAIN_TYPE_RIGHTTOP);
                }
            }

            mPos.x = pos.x;
            mPos.y = pos.y; // ��������
            return  false;

        default:
            MyResource.Assert(false , "Bullet does not have a direction!");
            break;
        } //  end switch

        MyResource.Assert(Map.INVALID != type1 && Map.INVALID != type2, "Wrong terrain");
        
        mPos.x = pos.x;
        mPos.y = pos.y; // ��������
        
        final int  type11 = rWorld.GetTerrain(destGridX, destGridY, Map.TERRAIN_TYPE_ALL);
        final int  type22 = rWorld.GetTerrain(anotherDestX, anotherDestY, Map.TERRAIN_TYPE_ALL);
        // ��������
        if (mOwner.mBulletMgr.GetPower() == BulletPower.BOOST &&
            (Map.IRON == type11 || Map.IRON == type22)) {
            Misc.PlaySound(AudioID.AUDIO_HIT);

            if (Map.IRON == type11)
                rWorld.ClearTerrain(destGridX, destGridY, Map.TERRAIN_TYPE_ALL);
                
            if (Map.IRON == type22)
                rWorld.ClearTerrain(anotherDestX, anotherDestY, Map.TERRAIN_TYPE_ALL);

            return   false;    
        }

        if (Map.IRON == type1 || Map.IRON == type2) { //  ���ܴ�����
            if (mOwner.GetType() == ObjectType.PLAYER)
                Misc.PlaySound(AudioID.AUDIO_HIT);
            return   false;    
        }

        if (rWorld.HitHeadquarters(this)) {  // �����ܲ�
            rWorld.DestroyHeadquarters();
            Misc.Vibrate(800);
            Misc.PlaySound(AudioID.AUDIO_BLAST);
            return   false;
        }
        
        return  true;
    }

    @Override
    boolean _TryMove(final Pos targetPos) {        
        // ���μ��
        if (!CheckTerrain(targetPos)) {
            OnHurt();
            return false;
        }

        // ��̬���
        if (GameWorld.Instance().HitTest(this)) {    
            return false;
        }

        return true;
    }
    
    /** ����Ƿ�ײ�϶Է���̹�˻����ӵ� */
    @Override
    boolean   HitTest(Movable pMovable) {
         if (null == pMovable ||
            !pMovable.IsAlive() || 
            !this.IsAlive() ||
            mOwner == pMovable)
             return false;
         
         // �ж�������״�Ƿ��ཻ
         if (!Rect.intersects(GetBodyRect(), pMovable.GetBodyRect()))
             return  false;

         ObjectType  myOwnerType  = mOwner.GetType();
         ObjectType  hisType = pMovable.GetType();
         
        // �ӵ�����
         if (hisType == ObjectType.BULLET) {
             Bullet  otherBullet = (Bullet)pMovable;
             if (myOwnerType != otherBullet.mOwner.GetType()) {
                 // ����ͬ��ģ�ֱ�ӵ���
                 _Vanish();
                 otherBullet._Vanish();
                 return  true;
             } else {
                 // ͬ����ӵ�����͸����
                 return  false;
             }
         }

         // ����̹��
         if (myOwnerType != hisType)   {
             
             // �Ƿ�ײ����ҵķ�����
             if (ObjectType.PLAYER == hisType && pMovable.IsGod()) {
                 _Vanish();

             } else {
                 
                 pMovable.OnHurt();
                 
                 if (pMovable.IsAlive()) {
                     // һǹ������̹�ˣ��ӵ�ֱ����ʧ
                     _Vanish();
                 } else {
                    // ͬ���ھ�
                     OnHurt();
                 }
             }
             return true;
             
         } else if (ObjectType.PLAYER == myOwnerType)  {
             return false; // ���Լ��� �ӵ����Դ�͸
         }

         return false; // �����ӵ����Դ�͸����̹��
    }
    
    private final Rect  mSrcRect = new Rect();
    private final RectF mDstRect = new RectF();
    private Paint mPaint = new Paint();
    Tank    mOwner;
}


final class BulletManager   {
    enum BulletPower {
        NORMAL,
        BOOST,
    };
    
    private Bullet[]        mBullets    = null;
    private int             mConcurrent = 1;
    private int             mSpeed      = Bullet.BULLET_SPEED;
    private BulletPower     mPower      = BulletPower.NORMAL;
    private int             mNextFireLoop  = 0;
    private static final int FIRE_INTERVAL = (GameWorld.FPS + 3) / 4;
    
    BulletManager(int nMaxBullet) {
        mBullets   = new Bullet[nMaxBullet];
        for (int i = 0; i < mBullets.length; ++ i) {
            mBullets[i] = new Bullet();
            mBullets[i].mID = i;
        }
    }
   
    /**  */
    void  Reset() {
        SetSpeed(Bullet.BULLET_SPEED);
        SetPower(BulletPower.NORMAL);
        SetConcurrent(1);
    }

    /**  */
    boolean CanFire() {
        final int aliveCnt =_GetLiveNum();
        if (aliveCnt == 0) {
            return true;
        }
        
        final int now = GameWorld.Instance().GetLoopCnt();
        if (aliveCnt < mConcurrent && now >= mNextFireLoop) {
            mNextFireLoop = now + FIRE_INTERVAL;
            return true;
        }
        
        if (BattleCity.smGameMode == GameMode.CLIENT) {
            for (Bullet bullet : mBullets) {
                Log.d(Bullet.TAG, "failed can fire state " + bullet.mState +
                        ", owner id " + (bullet.mOwner != null ? (bullet.mOwner.mID) : -100));
            }
            Log.e(Bullet.TAG, "Failed can fire, alive cnt " + aliveCnt);
        }

        return false;
    }

    void   SetSpeed(int speed) {
        mSpeed = speed;
    }
    
    int  GetSpeed()  {
        return  mSpeed;
    }
    
    /** ͬʱ�ܷ������ӵ� */
    void   SetConcurrent(int concurrent) {
        MyResource.Assert(concurrent > 0, "Invalid value");
        mConcurrent = concurrent;
    }
    
    int   GetConcurrent() {
        return   mConcurrent;
    }

    void  SetPower(BulletPower power) {
        mPower = power;
    }
    
    BulletPower GetPower() {
        return  mPower;
    }
    
    /** ��ǰ���м����ӵ� */
    private int   _GetLiveNum() {
        int cnt = 0;
        for (Bullet bullet : mBullets) {
            if (!bullet.IsValid()) {
                continue;
            }

            // Ϊ��ʹ�����ٶȿ�һ���
            if (bullet.GetState().ordinal() >= ObjectState.EXPLODE2.ordinal() &&
                bullet.GetState().ordinal() <= ObjectState.EXPLODE5.ordinal()) 
                continue;
                
            ++ cnt;
        }
        
        return cnt;
    }
    
    /** ��ȡһ�������ӵ� */
    Bullet  GetBullet() {
        MyResource.Assert(_GetLiveNum() <= mConcurrent, "Too many bullet, more than concurrent number");
        
        if (_GetLiveNum() == mConcurrent) {
            return  null;
        }
        
        for (Bullet bullet : mBullets) {
            if (!bullet.IsValid())   {
                return  bullet;
            }
        }
        
        if (BattleCity.smGameMode == GameMode.CLIENT) {
            for (Bullet bullet : mBullets) {
                Log.d(Bullet.TAG, "failed get bullet state " + bullet.mState +
                        ", owner id " + (bullet.mOwner != null ? (bullet.mOwner.mID) : -100));
            }
        }
        
        return  null;
    }    
    
    /**  */
    boolean  HitTest(Movable pObj) {
        for (Bullet bullet : mBullets) {
            if (pObj.IsAlive() &&
               !pObj.IsGod()  &&
                bullet.IsAlive() &&
                bullet.HitTest(pObj))
                return true;
        }

        return false;
    }
    
    void    Update() {
        for (Bullet  bullet : mBullets)
            bullet.Update();
    }
    
    void    Render(Canvas canvas) {
        for (Bullet bullet : mBullets)
            bullet.Paint(canvas);
    }
    
    /** �������� */
    void    ClearBullets() {
        mNextFireLoop = 0;
        for (Bullet bullet : mBullets)
            bullet.SetState(ObjectState.INVALID);
    }
}
