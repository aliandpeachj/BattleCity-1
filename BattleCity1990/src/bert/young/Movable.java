package bert.young;

abstract class Movable extends GameObject {    
    /** ������Ϊ��Ч�� �������·��� */
    private final  Pos      mTargetPos = new Pos();
    private final  Pos      mGrid      = new Pos();
    int   mID = -1;

    enum Dir {
        UP,
        DOWN,
        LEFT,
        RIGHT,
        NONE;
        
        static Dir Convert2Dir(int dir) {
            switch (dir) {
            case 0:
                return Dir.UP;
            case 1:
                return Dir.DOWN;
            case 2:
                return Dir.LEFT;
            case 3:
                return Dir.RIGHT;

            default:
                return Dir.NONE;
            }
        }
    };

    /** ��ʼ�� */
    public Movable() {  
        mDir    = Dir.NONE;     // ���ĳ����������о���
        mGrid.x = -1;
        mGrid.y = -1;
        mHP     = 0;
    }

    /** ��ȡ��ǰ���ڸ��� */
    Pos   GetGrid() {
        final GameWorld rWorld = GameWorld.Instance();
        mGrid.x = mPos.y / rWorld.GetGridWidth();
        mGrid.y = mPos.x / rWorld.GetGridWidth();
        
        MyResource.Assert(mGrid.x >= 0 && mGrid.y >= 0, "GetGrid ERROR");
        return  mGrid;
    }
    
    /** �ƶ� */
    void   UpdateMove() {
        if (Dir.NONE == mDir) {
            return;
        }

        /* �԰�������ƶ�������ԭ��ģ�
         * ����̹���ص�����̹�˻�����ʱ��ƫ�������ǰ�����ӳߴ�
         */
        int   nStep   = mSpeed / GameWorld.STEP;
        int   nRemain = mSpeed % GameWorld.STEP;

        for (int i = 0; i < nStep; ++ i)    {
            if (!_StepMove(GameWorld.STEP))
                return;
        }

        _StepMove(nRemain);
    }
    
    /**
     * 2012.01.14  ��ײ����
     * �����Ķ���������ײ�������ϸ��վ����ཻ������(��̹�˾���������Ƕ������)
     * �����Ϊ�麯��������������д
     */
    abstract boolean   HitTest(Movable pOther);

    /** ����������˶����򣬱�Ҫʱ�������� */
    final void   SetDir(Dir dir) {
        SetFaceDir(dir);
        mDir = dir;
    }
    
    final void  SetFaceDir(Dir dir) {
        if (dir != Dir.NONE) 
            mFaceDir = dir;
    }
    
    /** ����������ٶ� */
    final void  SetSpeed(int speed) {
        mSpeed = speed;
    }
    
    /** ��״̬��������ײ��⣬�ƶ����� */
    final boolean  IsAlive() {
        return  ObjectState.IsAlive(mState);
    }
    
    /** ��״̬�޵� */
    final boolean  IsGod()   {
        return ObjectState.IsGod(mState);
    }

    /** ��״̬��������Ϸ�߼� */
    final boolean  IsValid() {
         return ObjectState.IsValid(mState);
    }

    /** ��������ֵ */
    final void  SetHP(int hp) {
        mHP = hp;
    }

    /** �ж��Ƿ񳬹���ͼ�߽� */ 
    boolean  _TryMove(final Pos targetPos) {
        assert(GetBodySize() > 0);

        // �ӵ��Ƚ����ⲻ�ܵ����������
        final int sceneWidth = GameWorld.Instance().GetSceneWidth();
        if (targetPos.x < 0 ||
            targetPos.y < 0 ||
            targetPos.x + GetBodySize() > sceneWidth ||
            targetPos.y + GetBodySize() > sceneWidth) {
            return false;
        }

        return true;
    }
   
    /** ���˻ص� */
    abstract void    OnHurt();
 
    /** �����ƶ��Ǹ��̶��ķ���������Ҫ����ģ�巽�� _TryMove */
    private  boolean  _StepMove(int distCanMove) {
        MyResource.Assert(distCanMove <= GameWorld.STEP, "Move more than STEP");
        
        if (distCanMove == 0)
            return false;

        if (!IsAlive())
            return false;
        
        mTargetPos.x = mPos.x;
        mTargetPos.y = mPos.y;
        switch (mDir)  {
        case UP:
            mTargetPos.y -= distCanMove;
            break;
    
        case DOWN:
            mTargetPos.y += distCanMove;
            break;
    
        case LEFT:
            mTargetPos.x -= distCanMove;    
            break;

        case RIGHT:
            mTargetPos.x += distCanMove;
            break;

        default:
            return false;
        }

        return  _TryMove(mTargetPos);
    }
    
    Dir    mDir;
    Dir    mFaceDir;
    int    mHP;
    int    mSpeed;
}
