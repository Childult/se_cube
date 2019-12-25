package demo.ucas.src;

import java.util.Arrays;

class CubieCube {

    /**
     * 16 symmetries generated by S_F2, S_U4 and S_LR2
     */
    static CubieCube[] CubeSym = new CubieCube[16];

    /**
     * 18 move cubes
     */
    static CubieCube[] moveCube = new CubieCube[18];

    static long[] moveCubeSym = new long[18];
    static int[] firstMoveSym = new int[48];

    static int[][] SymMult = new int[16][16];
    static int[][] SymMultInv = new int[16][16];
    static int[][] SymMove = new int[16][18];
    static int[] Sym8Move = new int[8 * 18];
    static int[][] SymMoveUD = new int[16][18];

    /**
     * ClassIndexToRepresentantArrays
     */
    static char[] FlipS2R = new char[CoordCube.N_FLIP_SYM];
    static char[] TwistS2R = new char[CoordCube.N_TWIST_SYM];
    static char[] EPermS2R = new char[CoordCube.N_PERM_SYM];
    static byte[] Perm2CombP = new byte[CoordCube.N_PERM_SYM];
    static char[] PermInvEdgeSym = new char[CoordCube.N_PERM_SYM];
    static byte[] MPermInv = new byte[CoordCube.N_MPERM];

    /**
     * Notice that Edge Perm Coordnate and Corner Perm Coordnate are the same symmetry structure.
     * So their ClassIndexToRepresentantArray are the same.
     * And when x is RawEdgePermCoordnate, y*16+k is SymEdgePermCoordnate, y*16+(k^e2c[k]) will
     * be the SymCornerPermCoordnate of the State whose RawCornerPermCoordnate is x.
     */
    // static byte[] e2c = {0, 0, 0, 0, 1, 3, 1, 3, 1, 3, 1, 3, 0, 0, 0, 0};
    static final int SYM_E2C_MAGIC = 0x00DDDD00;
    static int ESym2CSym(int idx) {
        return idx ^ (SYM_E2C_MAGIC >> ((idx & 0xf) << 1) & 3);
    }

    /**
     * Raw-Coordnate to Sym-Coordnate, only for speeding up initializaion.
     */
    static char[] FlipR2S = new char[CoordCube.N_FLIP];
    static char[] TwistR2S = new char[CoordCube.N_TWIST];
    static char[] EPermR2S = new char[CoordCube.N_PERM];
    static char[] FlipS2RF = Search.USE_TWIST_FLIP_PRUN ? new char[CoordCube.N_FLIP_SYM * 8] : null;

    /**
     *
     */
    static char[] SymStateTwist;// = new char[CoordCube.N_TWIST_SYM];
    static char[] SymStateFlip;// = new char[CoordCube.N_FLIP_SYM];
    static char[] SymStatePerm;// = new char[CoordCube.N_PERM_SYM];

    static CubieCube urf1 = new CubieCube(2531, 1373, 67026819, 1367);
    static CubieCube urf2 = new CubieCube(2089, 1906, 322752913, 2040);
    static byte[][] urfMove = new byte[][] {
        {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17},
        {6, 7, 8, 0, 1, 2, 3, 4, 5, 15, 16, 17, 9, 10, 11, 12, 13, 14},
        {3, 4, 5, 6, 7, 8, 0, 1, 2, 12, 13, 14, 15, 16, 17, 9, 10, 11},
        {2, 1, 0, 5, 4, 3, 8, 7, 6, 11, 10, 9, 14, 13, 12, 17, 16, 15},
        {8, 7, 6, 2, 1, 0, 5, 4, 3, 17, 16, 15, 11, 10, 9, 14, 13, 12},
        {5, 4, 3, 8, 7, 6, 2, 1, 0, 14, 13, 12, 17, 16, 15, 11, 10, 9}
    };

    byte[] ca = {0, 1, 2, 3, 4, 5, 6, 7};
    byte[] ea = {0, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22};
    CubieCube temps = null;

    CubieCube() {
    }

    CubieCube(int cperm, int twist, int eperm, int flip) {
        this.setCPerm(cperm);
        this.setTwist(twist);
        Util.setNPerm(ea, eperm, 12, true);
        this.setFlip(flip);
    }

    CubieCube(CubieCube c) {
        copy(c);
    }

    void copy(CubieCube c) {
        for (int i = 0; i < 8; i++) {
            this.ca[i] = c.ca[i];
        }
        for (int i = 0; i < 12; i++) {
            this.ea[i] = c.ea[i];
        }
    }

    void invCubieCube() {
        if (temps == null) {
            temps = new CubieCube();
        }
        for (byte edge = 0; edge < 12; edge++) {
            temps.ea[ea[edge] >> 1] = (byte) (edge << 1 | ea[edge] & 1);
        }
        for (byte corn = 0; corn < 8; corn++) {
            temps.ca[ca[corn] & 0x7] = (byte) (corn | 0x20 >> (ca[corn] >> 3) & 0x18);
        }
        copy(temps);
    }

    /**
     * prod = a * b, Corner Only.
     */
    static void CornMult(CubieCube a, CubieCube b, CubieCube prod) {
        for (int corn = 0; corn < 8; corn++) {
            int oriA = a.ca[b.ca[corn] & 7] >> 3;
            int oriB = b.ca[corn] >> 3;
            prod.ca[corn] = (byte) (a.ca[b.ca[corn] & 7] & 7 | (oriA + oriB) % 3 << 3);
        }
    }

    /**
     * prod = a * b, Corner Only. With mirrored cases considered
     */
    static void CornMultFull(CubieCube a, CubieCube b, CubieCube prod) {
        for (int corn = 0; corn < 8; corn++) {
            int oriA = a.ca[b.ca[corn] & 7] >> 3;
            int oriB = b.ca[corn] >> 3;
            int ori = oriA + ((oriA < 3) ? oriB : 6 - oriB);
            ori = ori % 3 + ((oriA < 3) == (oriB < 3) ? 0 : 3);
            prod.ca[corn] = (byte) (a.ca[b.ca[corn] & 7] & 7 | ori << 3);
        }
    }

    /**
     * prod = a * b, Edge Only.
     */
    static void EdgeMult(CubieCube a, CubieCube b, CubieCube prod) {
        for (int ed = 0; ed < 12; ed++) {
            prod.ea[ed] = (byte) (a.ea[b.ea[ed] >> 1] ^ (b.ea[ed] & 1));
        }
    }

    /**
     * b = S_idx^-1 * a * S_idx, Corner Only.
     */
    static void CornConjugate(CubieCube a, int idx, CubieCube b) {
        CubieCube sinv = CubeSym[SymMultInv[0][idx]];
        CubieCube s = CubeSym[idx];
        for (int corn = 0; corn < 8; corn++) {
            int oriA = sinv.ca[a.ca[s.ca[corn] & 7] & 7] >> 3;
            int oriB = a.ca[s.ca[corn] & 7] >> 3;
            int ori = (oriA < 3) ? oriB : (3 - oriB) % 3;
            b.ca[corn] = (byte) (sinv.ca[a.ca[s.ca[corn] & 7] & 7] & 7 | ori << 3);
        }
    }

    /**
     * b = S_idx^-1 * a * S_idx, Edge Only.
     */
    static void EdgeConjugate(CubieCube a, int idx, CubieCube b) {
        CubieCube sinv = CubeSym[SymMultInv[0][idx]];
        CubieCube s = CubeSym[idx];
        for (int ed = 0; ed < 12; ed++) {
            b.ea[ed] = (byte) (sinv.ea[a.ea[s.ea[ed] >> 1] >> 1] ^ (a.ea[s.ea[ed] >> 1] & 1) ^ (s.ea[ed] & 1));
        }
    }

    static int getPermSymInv(int idx, int sym, boolean isCorner) {
        int idxi = PermInvEdgeSym[idx];
        if (isCorner) {
            idxi = ESym2CSym(idxi);
        }
        return idxi & 0xfff0 | SymMult[idxi & 0xf][sym];
    }

    static int getSkipMoves(long ssym) {
        int ret = 0;
        for (int i = 1; (ssym >>= 1) != 0; i++) {
            if ((ssym & 1) == 1) {
                ret |= firstMoveSym[i];
            }
        }
        return ret;
    }

    /**
     * this = S_urf^-1 * this * S_urf.
     */
    void URFConjugate() {
        if (temps == null) {
            temps = new CubieCube();
        }
        CornMult(urf2, this, temps);
        CornMult(temps, urf1, this);
        EdgeMult(urf2, this, temps);
        EdgeMult(temps, urf1, this);
    }

    // ********************************************* Get and set coordinates *********************************************
    // XSym : Symmetry Coordnate of X. MUST be called after initialization of ClassIndexToRepresentantArrays.

    // ++++++++++++++++++++ Phase 1 Coordnates ++++++++++++++++++++
    // Flip : Orientation of 12 Edges. Raw[0, 2048) Sym[0, 336 * 8)
    // Twist : Orientation of 8 Corners. Raw[0, 2187) Sym[0, 324 * 8)
    // UDSlice : Positions of the 4 UDSlice edges, the order is ignored. [0, 495)

    int getFlip() {
        int idx = 0;
        for (int i = 0; i < 11; i++) {
            idx = idx << 1 | ea[i] & 1;
        }
        return idx;
    }

    void setFlip(int idx) {
        int parity = 0, val;
        for (int i = 10; i >= 0; i--, idx >>= 1) {
            parity ^= (val = idx & 1);
            ea[i] = (byte) (ea[i] & ~1 | val);
        }
        ea[11] = (byte) (ea[11] & ~1 | parity);
    }

    int getFlipSym() {
        return FlipR2S[getFlip()];
    }

    int getTwist() {
        int idx = 0;
        for (int i = 0; i < 7; i++) {
            idx += (idx << 1) + (ca[i] >> 3);
        }
        return idx;
    }

    void setTwist(int idx) {
        int twst = 15, val;
        for (int i = 6; i >= 0; i--, idx /= 3) {
            twst -= (val = idx % 3);
            ca[i] = (byte) (ca[i] & 0x7 | val << 3);
        }
        ca[7] = (byte) (ca[7] & 0x7 | (twst % 3) << 3);
    }

    int getTwistSym() {
        return TwistR2S[getTwist()];
    }

    int getUDSlice() {
        return 494 - Util.getComb(ea, 8, true);
    }

    void setUDSlice(int idx) {
        Util.setComb(ea, 494 - idx, 8, true);
    }

    // ++++++++++++++++++++ Phase 2 Coordnates ++++++++++++++++++++
    // EPerm : Permutations of 8 UD Edges. Raw[0, 40320) Sym[0, 2187 * 16)
    // Cperm : Permutations of 8 Corners. Raw[0, 40320) Sym[0, 2187 * 16)
    // MPerm : Permutations of 4 UDSlice Edges. [0, 24)

    int getCPerm() {
        return Util.getNPerm(ca, 8, false);
    }

    void setCPerm(int idx) {
        Util.setNPerm(ca, idx, 8, false);
    }

    int getCPermSym() {
        return ESym2CSym(EPermR2S[getCPerm()]);
    }

    int getEPerm() {
        return Util.getNPerm(ea, 8, true);
    }

    void setEPerm(int idx) {
        Util.setNPerm(ea, idx, 8, true);
    }

    int getEPermSym() {
        return EPermR2S[getEPerm()];
    }

    int getMPerm() {
        return Util.getNPerm(ea, 12, true) % 24;
    }

    void setMPerm(int idx) {
        Util.setNPerm(ea, idx, 12, true);
    }

    int getCComb() {
        return Util.getComb(ca, 0, false);
    }

    void setCComb(int idx) {
        Util.setComb(ca, idx, 0, false);
    }

    /**
     * Check a cubiecube for solvability. Return the error code.
     * 0: Cube is solvable
     * -2: Not all 12 edges exist exactly once
     * -3: Flip error: One edge has to be flipped
     * -4: Not all corners exist exactly once
     * -5: Twist error: One corner has to be twisted
     * -6: Parity error: Two corners or two edges have to be exchanged
     */
    int verify() {
        int sum = 0;
        int edgeMask = 0;
        for (int e = 0; e < 12; e++) {
            edgeMask |= 1 << (ea[e] >> 1);
            sum ^= ea[e] & 1;
        }
        if (edgeMask != 0xfff) {
            return -2;// missing edges
        }
        if (sum != 0) {
            return -3;
        }
        int cornMask = 0;
        sum = 0;
        for (int c = 0; c < 8; c++) {
            cornMask |= 1 << (ca[c] & 7);
            sum += ca[c] >> 3;
        }
        if (cornMask != 0xff) {
            return -4;// missing corners
        }
        if (sum % 3 != 0) {
            return -5;// twisted corner
        }
        if ((Util.getNParity(Util.getNPerm(ea, 12, true), 12) ^ Util.getNParity(getCPerm(), 8)) != 0) {
            return -6;// parity error
        }
        return 0;// cube ok
    }

    long selfSymmetry() {
        CubieCube c = new CubieCube(this);
        CubieCube d = new CubieCube();
        int cperm = c.getCPermSym() >> 4;
        long sym = 0L;
        for (int urfInv = 0; urfInv < 6; urfInv++) {
            int cpermx = c.getCPermSym() >> 4;
            if (cperm == cpermx) {
                for (int i = 0; i < 16; i++) {
                    CornConjugate(c, SymMultInv[0][i], d);
                    if (Arrays.equals(d.ca, ca)) {
                        EdgeConjugate(c, SymMultInv[0][i], d);
                        if (Arrays.equals(d.ea, ea)) {
                            sym |= 1L << Math.min(urfInv << 4 | i, 48);
                        }
                    }
                }
            }
            c.URFConjugate();
            if (urfInv % 3 == 2) {
                c.invCubieCube();
            }
        }
        return sym;
    }

    // ********************************************* Initialization functions *********************************************

    static void initMove() {
        moveCube[0] = new CubieCube(15120, 0, 119750400, 0);
        moveCube[3] = new CubieCube(21021, 1494, 323403417, 0);
        moveCube[6] = new CubieCube(8064, 1236, 29441808, 550);
        moveCube[9] = new CubieCube(9, 0, 5880, 0);
        moveCube[12] = new CubieCube(1230, 412, 2949660, 0);
        moveCube[15] = new CubieCube(224, 137, 328552, 137);
        for (int a = 0; a < 18; a += 3) {
            for (int p = 0; p < 2; p++) {
                moveCube[a + p + 1] = new CubieCube();
                EdgeMult(moveCube[a + p], moveCube[a], moveCube[a + p + 1]);
                CornMult(moveCube[a + p], moveCube[a], moveCube[a + p + 1]);
            }
        }
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < 8; i++) {
            sb.append("|" + (ca[i] & 7) + " " + (ca[i] >> 3));
        }
        sb.append("\n");
        for (int i = 0; i < 12; i++) {
            sb.append("|" + (ea[i] >> 1) + " " + (ea[i] & 1));
        }
        return sb.toString();
    }

    static void initSym() {
        CubieCube c = new CubieCube();
        CubieCube d = new CubieCube();
        CubieCube t;

        CubieCube f2 = new CubieCube(28783, 0, 259268407, 0);
        CubieCube u4 = new CubieCube(15138, 0, 119765538, 7);
        CubieCube lr2 = new CubieCube(5167, 0, 83473207, 0);
        for (int i = 0; i < 8; i++) {
            lr2.ca[i] |= 3 << 3;
        }

        for (int i = 0; i < 16; i++) {
            CubeSym[i] = new CubieCube(c);
            CornMultFull(c, u4, d);
            EdgeMult(c, u4, d);
            t = d;  d = c;  c = t;
            if (i % 4 == 3) {
                CornMultFull(c, lr2, d);
                EdgeMult(c, lr2, d);
                t = d;  d = c;  c = t;
            }
            if (i % 8 == 7) {
                CornMultFull(c, f2, d);
                EdgeMult(c, f2, d);
                t = d;  d = c;  c = t;
            }
        }
        for (int i = 0; i < 16; i++) {
            for (int j = 0; j < 16; j++) {
                CornMultFull(CubeSym[i], CubeSym[j], c);
                for (int k = 0; k < 16; k++) {
                    if (Arrays.equals(CubeSym[k].ca, c.ca)) {
                        SymMult[i][j] = k; // SymMult[i][j] = (k ^ i ^ j ^ (0x14ab4 >> j & i << 1 & 2)));
                        SymMultInv[k][j] = i; // i * j = k => k * j^-1 = i
                        break;
                    }
                }
            }
        }
        for (int j = 0; j < 18; j++) {
            for (int s = 0; s < 16; s++) {
                CornConjugate(moveCube[j], SymMultInv[0][s], c);
                for (int m = 0; m < 18; m++) {
                    if (Arrays.equals(moveCube[m].ca, c.ca)) {
                        SymMove[s][j] = m;
                        SymMoveUD[s][Util.std2ud[j]] = Util.std2ud[m];
                        break;
                    }
                }
                if (s % 2 == 0) {
                    Sym8Move[j << 3 | s >> 1] = SymMove[s][j];
                }
            }
        }

        for (int i = 0; i < 18; i++) {
            moveCubeSym[i] = moveCube[i].selfSymmetry();
            int j = i;
            for (int s = 0; s < 48; s++) {
                if (SymMove[s % 16][j] < i) {
                    firstMoveSym[s] |= 1 << i;
                }
                if (s % 16 == 15) {
                    j = urfMove[2][j];
                }
            }
        }
    }

    static int initSym2Raw(final int N_RAW, char[] Sym2Raw, char[] Raw2Sym, char[] SymState, int coord) {
        final int N_RAW_HALF = (N_RAW + 1) / 2;
        CubieCube c = new CubieCube();
        CubieCube d = new CubieCube();
        int count = 0, idx = 0;
        int sym_inc = coord >= 2 ? 1 : 2;
        boolean isEdge = coord != 1;

        for (int i = 0; i < N_RAW; i++) {
            if (Raw2Sym[i] != 0) {
                continue;
            }
            switch (coord) {
            case 0: c.setFlip(i); break;
            case 1: c.setTwist(i); break;
            case 2: c.setEPerm(i); break;
            }
            for (int s = 0; s < 16; s += sym_inc) {
                if (isEdge) {
                    EdgeConjugate(c, s, d);
                } else {
                    CornConjugate(c, s, d);
                }
                switch (coord) {
                case 0: idx = d.getFlip();
                    break;
                case 1: idx = d.getTwist();
                    break;
                case 2: idx = d.getEPerm();
                    break;
                }
                if (coord == 0 && Search.USE_TWIST_FLIP_PRUN) {
                    FlipS2RF[count << 3 | s >> 1] = (char) idx;
                }
                if (idx == i) {
                    SymState[count] |= 1 << (s / sym_inc);
                }
                int symIdx = (count << 4 | s) / sym_inc;
                Raw2Sym[idx] = (char) symIdx;
            }
            Sym2Raw[count++] = (char) i;
        }
        return count;
    }

    static void initFlipSym2Raw() {
        initSym2Raw(CoordCube.N_FLIP, FlipS2R, FlipR2S,
                    SymStateFlip = new char[CoordCube.N_FLIP_SYM], 0);
    }

    static void initTwistSym2Raw() {
        initSym2Raw(CoordCube.N_TWIST, TwistS2R, TwistR2S,
                    SymStateTwist = new char[CoordCube.N_TWIST_SYM], 1);
    }

    static void initPermSym2Raw() {
        initSym2Raw(CoordCube.N_PERM, EPermS2R, EPermR2S,
                    SymStatePerm = new char[CoordCube.N_PERM_SYM], 2);
        CubieCube cc = new CubieCube();
        for (int i = 0; i < CoordCube.N_PERM_SYM; i++) {
            cc.setEPerm(EPermS2R[i]);
            Perm2CombP[i] = (byte) (Util.getComb(cc.ea, 0, true) + (Search.USE_COMBP_PRUN ? Util.getNParity(EPermS2R[i], 8) * 70 : 0));
            cc.invCubieCube();
            PermInvEdgeSym[i] = (char) cc.getEPermSym();
        }
        for (int i = 0; i < CoordCube.N_MPERM; i++) {
            cc.setMPerm(i);
            cc.invCubieCube();
            MPermInv[i] = (byte) cc.getMPerm();
        }
    }

    static {
        CubieCube.initMove();
        CubieCube.initSym();
    }
}
