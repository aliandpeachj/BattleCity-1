package bert.young;

import android.graphics.Canvas;
import android.graphics.Rect;

enum  ObjectState {
    INVALID,
    MINSTATE,

    EXPLODE1,
    EXPLODE2,
    EXPLODE3,
    EXPLODE4,
    EXPLODE5,
 
    /** �����ָ��� */
    NORMAL,

    GOD,

    BORN1,
    BORN2,
    BORN3,
    BORN4,
    PROTECT1,
    PROTECT2,
    MAXSTATE;
    
    /** ��״̬��������ײ��⡢�ƶ����� */
    static boolean  IsAlive(ObjectState    state) {
        // ����״̬���μ���ײ��Ҳ���μ��ƶ�
        return  state == NORMAL ||
                state == PROTECT1 ||
                state == PROTECT2;
    }
    
    /** ��״̬�޵� */
    static boolean   IsGod(ObjectState    state)   {
        return state.ordinal() > GOD.ordinal() &&
               state.ordinal() < MAXSTATE.ordinal();
    }
    
    /** ��״̬��������Ϸ�߼� */
    static boolean   IsValid(ObjectState state) { 
        return state.ordinal() > MINSTATE.ordinal() &&
               state.ordinal() < MAXSTATE.ordinal();
    }
}


/** �����ֻ��������λ�ã�����ߴ磬�򵥵ľ�����ײ */
abstract class GameObject {
    GameObject() {
        mState = ObjectState.INVALID;
    }
    
    enum ObjectType {
        PLAYER,
        ENEMY,
        BULLET,
        BONUS,
        HEAD,
    };
    
    abstract ObjectType  GetType();
    abstract int   GetBodySize();
    abstract void  Paint(Canvas canvas);
    abstract void  Update();
    
    static class Pos {
        int   x = 0;
        int   y = 0;
    }

    final Pos  GetPos() {
        return  mPos;
    }
    
    final void  SetPos(int x, int y) {
        mPos.x = x;
        mPos.y = y;
    }

    final void SetState(ObjectState newState) {
        mStateChanged  = true;
        mState         = newState;
    }

    final ObjectState  GetState() {
        return   mState;
    }

    final Rect  GetBodyRect() {
        mBodyRect.left     = mPos.x;
        mBodyRect.top      = mPos.y;
        mBodyRect.right    = mBodyRect.left + GetBodySize();
        mBodyRect.bottom   = mBodyRect.top  + GetBodySize(); 
        
        return mBodyRect;
    }
    
    boolean     mStateChanged = false;
    Pos         mPos = new Pos();
    ObjectState mState;
    
    private final  Rect  mBodyRect  = new Rect();
};
