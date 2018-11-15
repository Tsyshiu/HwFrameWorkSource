package org.bouncycastle.pqc.crypto.newhope;

import org.bouncycastle.crypto.tls.AlertDescription;

class NTT {
    private static final short[] BitReverseTable = new short[]{(short) 0, (short) 512, (short) 256, (short) 768, (short) 128, (short) 640, (short) 384, (short) 896, (short) 64, (short) 576, (short) 320, (short) 832, (short) 192, (short) 704, (short) 448, (short) 960, (short) 32, (short) 544, (short) 288, (short) 800, (short) 160, (short) 672, (short) 416, (short) 928, (short) 96, (short) 608, (short) 352, (short) 864, (short) 224, (short) 736, (short) 480, (short) 992, (short) 16, (short) 528, (short) 272, (short) 784, (short) 144, (short) 656, (short) 400, (short) 912, (short) 80, (short) 592, (short) 336, (short) 848, (short) 208, (short) 720, (short) 464, (short) 976, (short) 48, (short) 560, (short) 304, (short) 816, (short) 176, (short) 688, (short) 432, (short) 944, AlertDescription.unrecognized_name, (short) 624, (short) 368, (short) 880, (short) 240, (short) 752, (short) 496, (short) 1008, (short) 8, (short) 520, (short) 264, (short) 776, (short) 136, (short) 648, (short) 392, (short) 904, (short) 72, (short) 584, (short) 328, (short) 840, (short) 200, (short) 712, (short) 456, (short) 968, (short) 40, (short) 552, (short) 296, (short) 808, (short) 168, (short) 680, (short) 424, (short) 936, (short) 104, (short) 616, (short) 360, (short) 872, (short) 232, (short) 744, (short) 488, (short) 1000, (short) 24, (short) 536, (short) 280, (short) 792, (short) 152, (short) 664, (short) 408, (short) 920, (short) 88, (short) 600, (short) 344, (short) 856, (short) 216, (short) 728, (short) 472, (short) 984, (short) 56, (short) 568, (short) 312, (short) 824, (short) 184, (short) 696, (short) 440, (short) 952, (short) 120, (short) 632, (short) 376, (short) 888, (short) 248, (short) 760, (short) 504, (short) 1016, (short) 4, (short) 516, (short) 260, (short) 772, (short) 132, (short) 644, (short) 388, (short) 900, (short) 68, (short) 580, (short) 324, (short) 836, (short) 196, (short) 708, (short) 452, (short) 964, (short) 36, (short) 548, (short) 292, (short) 804, (short) 164, (short) 676, (short) 420, (short) 932, (short) 100, (short) 612, (short) 356, (short) 868, (short) 228, (short) 740, (short) 484, (short) 996, (short) 20, (short) 532, (short) 276, (short) 788, (short) 148, (short) 660, (short) 404, (short) 916, (short) 84, (short) 596, (short) 340, (short) 852, (short) 212, (short) 724, (short) 468, (short) 980, (short) 52, (short) 564, (short) 308, (short) 820, (short) 180, (short) 692, (short) 436, (short) 948, (short) 116, (short) 628, (short) 372, (short) 884, (short) 244, (short) 756, (short) 500, (short) 1012, (short) 12, (short) 524, (short) 268, (short) 780, (short) 140, (short) 652, (short) 396, (short) 908, (short) 76, (short) 588, (short) 332, (short) 844, (short) 204, (short) 716, (short) 460, (short) 972, (short) 44, (short) 556, (short) 300, (short) 812, (short) 172, (short) 684, (short) 428, (short) 940, (short) 108, (short) 620, (short) 364, (short) 876, (short) 236, (short) 748, (short) 492, (short) 1004, (short) 28, (short) 540, (short) 284, (short) 796, (short) 156, (short) 668, (short) 412, (short) 924, (short) 92, (short) 604, (short) 348, (short) 860, (short) 220, (short) 732, (short) 476, (short) 988, (short) 60, (short) 572, (short) 316, (short) 828, (short) 188, (short) 700, (short) 444, (short) 956, (short) 124, (short) 636, (short) 380, (short) 892, (short) 252, (short) 764, (short) 508, (short) 1020, (short) 2, (short) 514, (short) 258, (short) 770, (short) 130, (short) 642, (short) 386, (short) 898, (short) 66, (short) 578, (short) 322, (short) 834, (short) 194, (short) 706, (short) 450, (short) 962, (short) 34, (short) 546, (short) 290, (short) 802, (short) 162, (short) 674, (short) 418, (short) 930, (short) 98, (short) 610, (short) 354, (short) 866, (short) 226, (short) 738, (short) 482, (short) 994, (short) 18, (short) 530, (short) 274, (short) 786, (short) 146, (short) 658, (short) 402, (short) 914, (short) 82, (short) 594, (short) 338, (short) 850, (short) 210, (short) 722, (short) 466, (short) 978, (short) 50, (short) 562, (short) 306, (short) 818, (short) 178, (short) 690, (short) 434, (short) 946, AlertDescription.bad_certificate_hash_value, (short) 626, (short) 370, (short) 882, (short) 242, (short) 754, (short) 498, (short) 1010, (short) 10, (short) 522, (short) 266, (short) 778, (short) 138, (short) 650, (short) 394, (short) 906, (short) 74, (short) 586, (short) 330, (short) 842, (short) 202, (short) 714, (short) 458, (short) 970, (short) 42, (short) 554, (short) 298, (short) 810, (short) 170, (short) 682, (short) 426, (short) 938, (short) 106, (short) 618, (short) 362, (short) 874, (short) 234, (short) 746, (short) 490, (short) 1002, (short) 26, (short) 538, (short) 282, (short) 794, (short) 154, (short) 666, (short) 410, (short) 922, (short) 90, (short) 602, (short) 346, (short) 858, (short) 218, (short) 730, (short) 474, (short) 986, (short) 58, (short) 570, (short) 314, (short) 826, (short) 186, (short) 698, (short) 442, (short) 954, (short) 122, (short) 634, (short) 378, (short) 890, (short) 250, (short) 762, (short) 506, (short) 1018, (short) 6, (short) 518, (short) 262, (short) 774, (short) 134, (short) 646, (short) 390, (short) 902, (short) 70, (short) 582, (short) 326, (short) 838, (short) 198, (short) 710, (short) 454, (short) 966, (short) 38, (short) 550, (short) 294, (short) 806, (short) 166, (short) 678, (short) 422, (short) 934, (short) 102, (short) 614, (short) 358, (short) 870, (short) 230, (short) 742, (short) 486, (short) 998, (short) 22, (short) 534, (short) 278, (short) 790, (short) 150, (short) 662, (short) 406, (short) 918, (short) 86, (short) 598, (short) 342, (short) 854, (short) 214, (short) 726, (short) 470, (short) 982, (short) 54, (short) 566, (short) 310, (short) 822, (short) 182, (short) 694, (short) 438, (short) 950, (short) 118, (short) 630, (short) 374, (short) 886, (short) 246, (short) 758, (short) 502, (short) 1014, (short) 14, (short) 526, (short) 270, (short) 782, (short) 142, (short) 654, (short) 398, (short) 910, (short) 78, (short) 590, (short) 334, (short) 846, (short) 206, (short) 718, (short) 462, (short) 974, (short) 46, (short) 558, (short) 302, (short) 814, (short) 174, (short) 686, (short) 430, (short) 942, AlertDescription.unsupported_extension, (short) 622, (short) 366, (short) 878, (short) 238, (short) 750, (short) 494, (short) 1006, (short) 30, (short) 542, (short) 286, (short) 798, (short) 158, (short) 670, (short) 414, (short) 926, (short) 94, (short) 606, (short) 350, (short) 862, (short) 222, (short) 734, (short) 478, (short) 990, (short) 62, (short) 574, (short) 318, (short) 830, (short) 190, (short) 702, (short) 446, (short) 958, (short) 126, (short) 638, (short) 382, (short) 894, (short) 254, (short) 766, (short) 510, (short) 1022, (short) 1, (short) 513, (short) 257, (short) 769, (short) 129, (short) 641, (short) 385, (short) 897, (short) 65, (short) 577, (short) 321, (short) 833, (short) 193, (short) 705, (short) 449, (short) 961, (short) 33, (short) 545, (short) 289, (short) 801, (short) 161, (short) 673, (short) 417, (short) 929, (short) 97, (short) 609, (short) 353, (short) 865, (short) 225, (short) 737, (short) 481, (short) 993, (short) 17, (short) 529, (short) 273, (short) 785, (short) 145, (short) 657, (short) 401, (short) 913, (short) 81, (short) 593, (short) 337, (short) 849, (short) 209, (short) 721, (short) 465, (short) 977, (short) 49, (short) 561, (short) 305, (short) 817, (short) 177, (short) 689, (short) 433, (short) 945, AlertDescription.bad_certificate_status_response, (short) 625, (short) 369, (short) 881, (short) 241, (short) 753, (short) 497, (short) 1009, (short) 9, (short) 521, (short) 265, (short) 777, (short) 137, (short) 649, (short) 393, (short) 905, (short) 73, (short) 585, (short) 329, (short) 841, (short) 201, (short) 713, (short) 457, (short) 969, (short) 41, (short) 553, (short) 297, (short) 809, (short) 169, (short) 681, (short) 425, (short) 937, (short) 105, (short) 617, (short) 361, (short) 873, (short) 233, (short) 745, (short) 489, (short) 1001, (short) 25, (short) 537, (short) 281, (short) 793, (short) 153, (short) 665, (short) 409, (short) 921, (short) 89, (short) 601, (short) 345, (short) 857, (short) 217, (short) 729, (short) 473, (short) 985, (short) 57, (short) 569, (short) 313, (short) 825, (short) 185, (short) 697, (short) 441, (short) 953, (short) 121, (short) 633, (short) 377, (short) 889, (short) 249, (short) 761, (short) 505, (short) 1017, (short) 5, (short) 517, (short) 261, (short) 773, (short) 133, (short) 645, (short) 389, (short) 901, (short) 69, (short) 581, (short) 325, (short) 837, (short) 197, (short) 709, (short) 453, (short) 965, (short) 37, (short) 549, (short) 293, (short) 805, (short) 165, (short) 677, (short) 421, (short) 933, (short) 101, (short) 613, (short) 357, (short) 869, (short) 229, (short) 741, (short) 485, (short) 997, (short) 21, (short) 533, (short) 277, (short) 789, (short) 149, (short) 661, (short) 405, (short) 917, (short) 85, (short) 597, (short) 341, (short) 853, (short) 213, (short) 725, (short) 469, (short) 981, (short) 53, (short) 565, (short) 309, (short) 821, (short) 181, (short) 693, (short) 437, (short) 949, (short) 117, (short) 629, (short) 373, (short) 885, (short) 245, (short) 757, (short) 501, (short) 1013, (short) 13, (short) 525, (short) 269, (short) 781, (short) 141, (short) 653, (short) 397, (short) 909, (short) 77, (short) 589, (short) 333, (short) 845, (short) 205, (short) 717, (short) 461, (short) 973, (short) 45, (short) 557, (short) 301, (short) 813, (short) 173, (short) 685, (short) 429, (short) 941, (short) 109, (short) 621, (short) 365, (short) 877, (short) 237, (short) 749, (short) 493, (short) 1005, (short) 29, (short) 541, (short) 285, (short) 797, (short) 157, (short) 669, (short) 413, (short) 925, (short) 93, (short) 605, (short) 349, (short) 861, (short) 221, (short) 733, (short) 477, (short) 989, (short) 61, (short) 573, (short) 317, (short) 829, (short) 189, (short) 701, (short) 445, (short) 957, (short) 125, (short) 637, (short) 381, (short) 893, (short) 253, (short) 765, (short) 509, (short) 1021, (short) 3, (short) 515, (short) 259, (short) 771, (short) 131, (short) 643, (short) 387, (short) 899, (short) 67, (short) 579, (short) 323, (short) 835, (short) 195, (short) 707, (short) 451, (short) 963, (short) 35, (short) 547, (short) 291, (short) 803, (short) 163, (short) 675, (short) 419, (short) 931, (short) 99, (short) 611, (short) 355, (short) 867, (short) 227, (short) 739, (short) 483, (short) 995, (short) 19, (short) 531, (short) 275, (short) 787, (short) 147, (short) 659, (short) 403, (short) 915, (short) 83, (short) 595, (short) 339, (short) 851, (short) 211, (short) 723, (short) 467, (short) 979, (short) 51, (short) 563, (short) 307, (short) 819, (short) 179, (short) 691, (short) 435, (short) 947, AlertDescription.unknown_psk_identity, (short) 627, (short) 371, (short) 883, (short) 243, (short) 755, (short) 499, (short) 1011, (short) 11, (short) 523, (short) 267, (short) 779, (short) 139, (short) 651, (short) 395, (short) 907, (short) 75, (short) 587, (short) 331, (short) 843, (short) 203, (short) 715, (short) 459, (short) 971, (short) 43, (short) 555, (short) 299, (short) 811, (short) 171, (short) 683, (short) 427, (short) 939, (short) 107, (short) 619, (short) 363, (short) 875, (short) 235, (short) 747, (short) 491, (short) 1003, (short) 27, (short) 539, (short) 283, (short) 795, (short) 155, (short) 667, (short) 411, (short) 923, (short) 91, (short) 603, (short) 347, (short) 859, (short) 219, (short) 731, (short) 475, (short) 987, (short) 59, (short) 571, (short) 315, (short) 827, (short) 187, (short) 699, (short) 443, (short) 955, (short) 123, (short) 635, (short) 379, (short) 891, (short) 251, (short) 763, (short) 507, (short) 1019, (short) 7, (short) 519, (short) 263, (short) 775, (short) 135, (short) 647, (short) 391, (short) 903, (short) 71, (short) 583, (short) 327, (short) 839, (short) 199, (short) 711, (short) 455, (short) 967, (short) 39, (short) 551, (short) 295, (short) 807, (short) 167, (short) 679, (short) 423, (short) 935, (short) 103, (short) 615, (short) 359, (short) 871, (short) 231, (short) 743, (short) 487, (short) 999, (short) 23, (short) 535, (short) 279, (short) 791, (short) 151, (short) 663, (short) 407, (short) 919, (short) 87, (short) 599, (short) 343, (short) 855, (short) 215, (short) 727, (short) 471, (short) 983, (short) 55, (short) 567, (short) 311, (short) 823, (short) 183, (short) 695, (short) 439, (short) 951, (short) 119, (short) 631, (short) 375, (short) 887, (short) 247, (short) 759, (short) 503, (short) 1015, (short) 15, (short) 527, (short) 271, (short) 783, (short) 143, (short) 655, (short) 399, (short) 911, (short) 79, (short) 591, (short) 335, (short) 847, (short) 207, (short) 719, (short) 463, (short) 975, (short) 47, (short) 559, (short) 303, (short) 815, (short) 175, (short) 687, (short) 431, (short) 943, AlertDescription.certificate_unobtainable, (short) 623, (short) 367, (short) 879, (short) 239, (short) 751, (short) 495, (short) 1007, (short) 31, (short) 543, (short) 287, (short) 799, (short) 159, (short) 671, (short) 415, (short) 927, (short) 95, (short) 607, (short) 351, (short) 863, (short) 223, (short) 735, (short) 479, (short) 991, (short) 63, (short) 575, (short) 319, (short) 831, (short) 191, (short) 703, (short) 447, (short) 959, (short) 127, (short) 639, (short) 383, (short) 895, (short) 255, (short) 767, (short) 511, (short) 1023};

    NTT() {
    }

    static void bitReverse(short[] sArr) {
        for (short s = (short) 0; s < (short) 1024; s++) {
            short s2 = BitReverseTable[s];
            if (s < s2) {
                short s3 = sArr[s];
                sArr[s] = sArr[s2];
                sArr[s2] = s3;
            }
        }
    }

    static void core(short[] sArr, short[] sArr2) {
        for (int i = 0; i < 10; i += 2) {
            int i2;
            int i3;
            int i4;
            int i5;
            int i6;
            int i7;
            int i8 = 1 << i;
            for (i2 = 0; i2 < i8; i2++) {
                i3 = i2;
                i4 = 0;
                while (i3 < 1023) {
                    i5 = sArr[i3] & 65535;
                    i6 = i3 + i8;
                    i7 = sArr[i6] & 65535;
                    int i9 = i4 + 1;
                    short s = sArr2[i4];
                    sArr[i3] = (short) (i5 + i7);
                    sArr[i6] = Reduce.montgomery(s * ((i5 + 36867) - i7));
                    i3 += 2 * i8;
                    i4 = i9;
                }
            }
            int i10 = i8 << 1;
            for (i8 = 0; i8 < i10; i8++) {
                i2 = i8;
                i3 = 0;
                while (i2 < 1023) {
                    i4 = sArr[i2] & 65535;
                    i5 = i2 + i10;
                    i6 = sArr[i5] & 65535;
                    i7 = i3 + 1;
                    short s2 = sArr2[i3];
                    sArr[i2] = Reduce.barrett((short) (i4 + i6));
                    sArr[i5] = Reduce.montgomery(s2 * ((i4 + 36867) - i6));
                    i2 += 2 * i10;
                    i3 = i7;
                }
            }
        }
    }

    static void mulCoefficients(short[] sArr, short[] sArr2) {
        for (int i = 0; i < 1024; i++) {
            sArr[i] = Reduce.montgomery((sArr[i] & 65535) * (65535 & sArr2[i]));
        }
    }
}