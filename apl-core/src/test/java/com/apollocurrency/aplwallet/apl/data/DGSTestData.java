/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.data;

import com.apollocurrency.aplwallet.apl.core.dgs.model.DGSFeedback;
import com.apollocurrency.aplwallet.apl.core.dgs.model.DGSGoods;
import com.apollocurrency.aplwallet.apl.core.dgs.model.DGSPublicFeedback;
import com.apollocurrency.aplwallet.apl.core.dgs.model.DGSPurchase;
import com.apollocurrency.aplwallet.apl.core.dgs.model.DGSTag;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;

import java.util.List;

public class DGSTestData {


    public final DGSPublicFeedback PUBLIC_FEEDBACK_0  =  createFeedback(1, 100,                  "Deleted feedback",       541960, false);
    public final DGSPublicFeedback PUBLIC_FEEDBACK_1  =  createFeedback(2, 7052449049531083429L, "Feedback message",        541965, false);
    public final DGSPublicFeedback PUBLIC_FEEDBACK_2  =  createFeedback(3, -3910276708092716563L, "Goods dgs",              542677, false);
    public final DGSPublicFeedback PUBLIC_FEEDBACK_3  =  createFeedback(4, -3910276708092716563L, "Goods dgs",              542679, true);
    public final DGSPublicFeedback PUBLIC_FEEDBACK_4  =  createFeedback(5, -3910276708092716563L, "Test goods feedback 2",  542679, true);
    public final DGSPublicFeedback PUBLIC_FEEDBACK_5  =  createFeedback(6, -1155069143692520623L, "Public feedback",        542693, false);
    public final DGSPublicFeedback PUBLIC_FEEDBACK_6  =  createFeedback(7, -1155069143692520623L, "Public feedback",        542695, true);
    public final DGSPublicFeedback PUBLIC_FEEDBACK_7  =  createFeedback(8, -1155069143692520623L, "Another Public feedback",542695, true);
    public final DGSPublicFeedback PUBLIC_FEEDBACK_8  =  createFeedback(9, 100,                   "Deleted feedback",       542695, false);
    public final DGSPublicFeedback PUBLIC_FEEDBACK_9 =  createFeedback(10, 7052449049531083429L, "Feedback message",      542799, false);
    public final DGSPublicFeedback PUBLIC_FEEDBACK_10  =  createFeedback(11, 7052449049531083429L, "Public feedback 2",      542799, false);
    public final DGSPublicFeedback PUBLIC_FEEDBACK_11 =  createFeedback(12, 7052449049531083429L, "Feedback message",      542801, true);
    public final DGSPublicFeedback PUBLIC_FEEDBACK_12 =  createFeedback(13, 7052449049531083429L, "Public feedback 2",     542801, true);
    public final DGSPublicFeedback PUBLIC_FEEDBACK_13 =  createFeedback(14, 7052449049531083429L, "Public feedback 3",     542801, true);

    public final DGSPublicFeedback NEW_PUBLIC_FEEDBACK_0 = new DGSPublicFeedback(PUBLIC_FEEDBACK_13.getDbId() + 1, PUBLIC_FEEDBACK_13.getHeight() + 1, "NewFeedback-1", PUBLIC_FEEDBACK_13.getId());
    public final DGSPublicFeedback NEW_PUBLIC_FEEDBACK_1 = new DGSPublicFeedback(PUBLIC_FEEDBACK_13.getDbId() + 2, PUBLIC_FEEDBACK_13.getHeight() + 1, "NewFeedback-2", PUBLIC_FEEDBACK_13.getId());
    public final DGSPublicFeedback NEW_PUBLIC_FEEDBACK_2 = new DGSPublicFeedback(PUBLIC_FEEDBACK_13.getDbId() + 3, PUBLIC_FEEDBACK_13.getHeight() + 1, "NewFeedback-3", PUBLIC_FEEDBACK_13.getId());

    public final List<DGSPublicFeedback> PURCHASE_0_PUBLIC_FEEDBACKS = List.of(PUBLIC_FEEDBACK_11, PUBLIC_FEEDBACK_12, PUBLIC_FEEDBACK_13);
    public final List<DGSPublicFeedback> PURCHASE_6_PUBLIC_FEEDBACKS = List.of(PUBLIC_FEEDBACK_3, PUBLIC_FEEDBACK_4);
    public final List<DGSPublicFeedback> PURCHASE_7_PUBLIC_FEEDBACKS = List.of(PUBLIC_FEEDBACK_6, PUBLIC_FEEDBACK_7);

    public final DGSFeedback FEEDBACK_0  = createFeedback(10	    ,7052449049531083429L	,"5bfe10ef4af446d18145113d2df6e35ef9a154d2db25f035ab94b243c12e498790de649e528af993eb786d10d56ea6190d081c9d379a75009a20c905912d1f89"	                                ,"469c3ccdcc8e5dd8bdc5e1c4ff83ee5dcf4020f2eafbaf6e74bb24c4a7f65bd2"	,542010	,false);
    public final DGSFeedback FEEDBACK_1  = createFeedback(20	    ,200                	    ,"c7673f0a7f3ced8db457620b3d3d34f873cbeb1db7ffe3faea83e03b8973ef52eefcfa3651b1c6d926579e0c3eddb4e88695ad436b319b2d81ad5ce7ba0751ca"	                                ,"1ade60abe93f5cfce908a10bb2d1b474f024779843faba4bb1f2ef06f277d3c7"	,542010	,false);
    public final DGSFeedback FEEDBACK_2  = createFeedback(30	    ,7052449049531083429L	,"5bfe10ef4af446d18145113d2df6e35ef9a154d2db25f035ab94b243c12e498790de649e528af993eb786d10d56ea6190d081c9d379a75009a20c905912d1f89"	                                ,"469c3ccdcc8e5dd8bdc5e1c4ff83ee5dcf4020f2eafbaf6e74bb24c4a7f65bd2"	,542012	,false);
    public final DGSFeedback FEEDBACK_3  = createFeedback(40	    ,7052449049531083429L	,"9838f3543b3f91a9ad21bde160ae1fd195b8534fe85ed1a587b27d0b8c7385dc6ad22914a2bf3693158f99026e102e8a2137ba1ecb1b81ac56f4bcfd19726f4a"	                                ,"b45128a765486610a728f92e9cb9204d27c712a8862c3679880c0f0cdfad4605"	,542012	,false);
    public final DGSFeedback FEEDBACK_4  = createFeedback(50	    ,7052449049531083429L	,"5bfe10ef4af446d18145113d2df6e35ef9a154d2db25f035ab94b243c12e498790de649e528af993eb786d10d56ea6190d081c9d379a75009a20c905912d1f89"	                                ,"469c3ccdcc8e5dd8bdc5e1c4ff83ee5dcf4020f2eafbaf6e74bb24c4a7f65bd2"	,542014	,true );
    public final DGSFeedback FEEDBACK_5  = createFeedback(60	    ,7052449049531083429L	,"9838f3543b3f91a9ad21bde160ae1fd195b8534fe85ed1a587b27d0b8c7385dc6ad22914a2bf3693158f99026e102e8a2137ba1ecb1b81ac56f4bcfd19726f4a"	                                ,"b45128a765486610a728f92e9cb9204d27c712a8862c3679880c0f0cdfad4605"	,542014	,true );
    public final DGSFeedback FEEDBACK_6  = createFeedback(70	    ,7052449049531083429L	,"f7f482fa35b1749ff8a095be76b5aa9260d3445990444a50bc3538b6c6c03bf46f0974fe4d02feb1d88569c15db8f380dbec193c71d8663e845110280dd23154"	                                ,"4828b6f8e8d6854798386455c2094e90d65e9a682ef5add71965f6488b586aad"	,542014	,true );
    public final DGSFeedback FEEDBACK_7  = createFeedback(80	    ,200                	    ,"c7673f0a7f3ced8db457620b3d3d34f873cbeb1db7ffe3faea83e03b8973ef52eefcfa3651b1c6d926579e0c3eddb4e88695ad436b319b2d81ad5ce7ba0751ca"	                                ,"1ade60abe93f5cfce908a10bb2d1b474f024779843faba4bb1f2ef06f277d3c7"	,542150	,false);
    public final DGSFeedback FEEDBACK_8  = createFeedback(90	    ,-3910276708092716563L	,"33c6b12600b8dba6caa21887fc4f17702d2d841395d60b7d8435b8cc8cd1e2755ed177db220f7768b4f7d26ec1abd014ad953e8f00294e7615d16186037572eabe61c17c3c71a21846d8c7b08f9be85f"	,"955484fec03af1f97fa14e092c8d77e10c84a47bef9e5918b887f18760572429"	,542682	,true );
    public final DGSFeedback FEEDBACK_9  = createFeedback(100	,-1155069143692520623L	,"2ebec2f061d3b54cdfc7b8fac33762c90e37fa2bb61450726dae0425b28bbfb3dbcaf6259390abe44537f14a3ff35c08487ad65a7050082ec3ce76413aefb715f5044ff866705d1cfd4af01391f752a2"	,"bce4d461fe18c95e6147266f8852e989a413248963828a136380f98f5fde8e44"	,542688	,false);
    public final DGSFeedback FEEDBACK_10 = createFeedback(110	,-1155069143692520623L	,"2ebec2f061d3b54cdfc7b8fac33762c90e37fa2bb61450726dae0425b28bbfb3dbcaf6259390abe44537f14a3ff35c08487ad65a7050082ec3ce76413aefb715f5044ff866705d1cfd4af01391f752a2"	,"bce4d461fe18c95e6147266f8852e989a413248963828a136380f98f5fde8e44"	,542690	,true );
    public final DGSFeedback FEEDBACK_11 = createFeedback(120	,-1155069143692520623L	,"eb07b583e5c2f52dae03872b17d31346e97b2fa1f21ad2b92f9c2a3bf349bced1780eb2858ab85035b8d437eda92a4665b3acb992bec126102a21157b785f64c"                                	,"7f4fb46c93dc9e9c18d242fdf9be3913f38934913cac5ae682c4a59bb40a2be5"	,542690	,true );


    public final DGSFeedback NEW_FEEDBACK_0  = createFeedback(FEEDBACK_11.getDbId() + 1	,	150,"2f3e999894566865ff7fb3378c844504a0c4c218aa5885c50b598f6e4fb3cf0ca4415dfc8692699dbc519aa3759de192521f7544bcf004e944cec98601f8f9d8"	,"f924eadc20fcc91f7dd6a015b9f30d3269d20ec940107560d2f1081f2278ea38"	,543000	,true);
    public final DGSFeedback NEW_FEEDBACK_1 = createFeedback(FEEDBACK_11.getDbId() + 2	,	150,"a78481d5bc90acce22e107d1e12f3d339112767d0bd5421dc56f2ee9dda95521a710534adefe2f4d3990aacc73b6b6fd8cf5248cd3635326ebac15105d507c1c"	,"4b7d1f07378befdabe71f81d7deabad94858782f3551f4c29c1ee74a6cf74a09"	,543000	,true );
    public final DGSFeedback NEW_FEEDBACK_2 = createFeedback(FEEDBACK_11.getDbId() + 3	,   150,"752f9aa32bad466920bc52e924fc48808922484b867a7b839dd75e52949344356e2f368b273accc1edcb04badb9362f38f436e4cf2a1fe2b48109219f2c1aa03" ,"ab38549b6231be92061d2be517ed5a2e348ab1681fa2da74e4e70178ad19e655"	,543000	,true );



    public final List<DGSFeedback> PURCHASE_0_FEEDBACKS = List.of(FEEDBACK_4, FEEDBACK_5, FEEDBACK_6);
    public final List<DGSFeedback> PURCHASE_6_FEEDBACKS = List.of(FEEDBACK_8);
    public final List<DGSFeedback> PURCHASE_7_FEEDBACKS = List.of(FEEDBACK_10, FEEDBACK_11);


    public final DGSPurchase PURCHASE_0  = createPurchase(50	,7052449049531083429L,   3705364957971254799L,350597963087434976L	        ,9211698109297098287L,1	,100000000000L	,(short)173056826	,null	,null	,41815212	,false	,"d0e9017bfa6d02bf823e2cc5973f1ce8146ed7155ac326afb77a41d4ff2737f134a28e29be0c01357aee9660dca00a37"	                                ,"47d7e125d33917ad2efe5cb34c754816541b3862056cae7672e8faea3fdb6c97"	,true	        ,null	        ,null	        ,false	                ,false	                 ,100000000	,0	        ,541915	    ,false,  null, null);
    public final DGSPurchase PURCHASE_1  = createPurchase(51	,7938514984365421132L,   3705364957971254799L,350597963087434976L	        ,9211698109297098287L,1	,100000000000L	,(short)173056760	,null	,null	,41815146	,false	,"cf07cf9eef82aa48108a0ed23b17178ff7d4c915d589a8adaa9775f50777e47ec3b73067b53c924c0f75c4487351f37e"	                                ,"53da1333f562e1e33ec6294d050000a538d1ca3471e86cd49f2285aa98057236"	,true	        ,null	        ,null	        ,false	                ,false	                 ,300000000	,0	        ,541918	    ,false, null, null);
    public final DGSPurchase PURCHASE_2  = createPurchase(52	,5168710752758706151L,   7821792282123976600L,-9127861922199955586L	    ,9211698109297098287L,1	,500000000	    ,(short)171922753	,null	,null	,41282338	,false	,"173721d4b9d18494394bd55ceadb1ca8a6e305f3e960378527c9c5f9d27a7a8d914afbbc5dca715ca96db70b3ce7f45e"	                                ,"39b7a5d586c226a83a3f336cf3f9644f671c448afa7c6c076eaf941e5e3acb17"	,true	        ,null	        ,null	        ,false	                ,false	                 ,200000000	,0	        ,541921	    ,true , null, null);
    public final DGSPurchase PURCHASE_3  = createPurchase(53	,2646157157844538473L,   9211698109297098287L,-3940436337202836661L	    ,9211698109297098287L,1	,10000000000L	,(short)167789481	,null	,null	,36547867	,false	,"0d6a37a914fbb9694d7d80fc1fdb782503e42f6b5fb36ebefa1e29fdb2c45c68f608c8d8b48b169ce7b030c8fb7ad780"	                                ,"d35a6bfb98047c9a979fb0865e18f41242e5389e15e81be4843118c45430300b"	,true	        ,null	        ,null	        ,false	                ,false	                 ,0	        ,0	        ,541923	    ,true , null, null);
    public final DGSPurchase PURCHASE_4  = createPurchase(54	,7052449049531083429L,   3705364957971254799L,350597963087434976L	        ,9211698109297098287L,1	,100000000000L	,(short)173056826	,null	,null	,41815212	,false	,"d0e9017bfa6d02bf823e2cc5973f1ce8146ed7155ac326afb77a41d4ff2737f134a28e29be0c01357aee9660dca00a37"	                                ,"47d7e125d33917ad2efe5cb34c754816541b3862056cae7672e8faea3fdb6c97"	,true	        ,null	        ,null	        ,false	                ,true	                 ,100000000	,0	        ,541965	    ,false, PURCHASE_0_PUBLIC_FEEDBACKS, null);
    public final DGSPurchase PURCHASE_5  = createPurchase(55	,7052449049531083429L,   3705364957971254799L,350597963087434976L	        ,9211698109297098287L,1	,100000000000L	,(short)173056826	,null	,null	,41815212	,false	,"d0e9017bfa6d02bf823e2cc5973f1ce8146ed7155ac326afb77a41d4ff2737f134a28e29be0c01357aee9660dca00a37"	                                ,"47d7e125d33917ad2efe5cb34c754816541b3862056cae7672e8faea3fdb6c97"	,true	        ,null	        ,null	        ,true	                ,true	                 ,100000000	,0	        ,542010	    ,true , PURCHASE_0_PUBLIC_FEEDBACKS, PURCHASE_0_FEEDBACKS);
    public final DGSPurchase PURCHASE_6  = createPurchase(56	,-3910276708092716563L	,3705364957971254799L,-1443882373390424300L	    ,9211698109297098287L,1	,100000000	    ,(short)173058296	,null	,null	,41816681	,true	,null	                                                                                                                                ,null	                                                            ,false	    ,null	        ,null	        ,false	                ,false	                 ,0	        ,0	        ,542026	    ,false, null, null);
    public final DGSPurchase PURCHASE_7  = createPurchase(57	,-1155069143692520623L	,3705364957971254799L,-2208439159357779035L	    ,9211698109297098287L,1	,100000000	    ,(short)173058317	,null	,null	,41816705	,true	,null	                                                                                                                                ,null	                                                            ,false	    ,null	        ,null	        ,false	                ,false	                 ,0	        ,0	        ,542029	    ,false, null, null);
    public final DGSPurchase PURCHASE_8  = createPurchase(58	,7938514984365421132L,   3705364957971254799L, 350597963087434976L	    ,9211698109297098287L,1	,100000000000L	,(short)173056760	,null	,null	,41815146	,false	,"cf07cf9eef82aa48108a0ed23b17178ff7d4c915d589a8adaa9775f50777e47ec3b73067b53c924c0f75c4487351f37e"	                                ,"53da1333f562e1e33ec6294d050000a538d1ca3471e86cd49f2285aa98057236"	,true	        ,null	        ,null	        ,false	                ,false	                 ,300000000	,100000000	,542650	    ,true , null, null);
    public final DGSPurchase PURCHASE_9  = createPurchase(59	,-1155069143692520623L	,3705364957971254799L,-2208439159357779035L	    ,9211698109297098287L,1	,100000000	    ,(short)173058317	,null	,null	,41816705	,false	,"a83ed3a4798ac7629345f0204383f8187209a0db6167048b5634178d5c1e450e90d363068ddc65d93e3b3c4050e52c40803503388dce82d2e735f1a3fcb2086a"	,"be123e5322ab64bbdf047c89c1010bd11d0ec262207c033325e8dda3a185f6b8"	,true	        ,null	        ,null	        ,false	                ,false	                 ,0	        ,0	        ,542658	    ,false, null, null);
    public final DGSPurchase PURCHASE_10 = createPurchase(60	,-3910276708092716563L	,3705364957971254799L,-1443882373390424300L	    ,9211698109297098287L,1	,100000000	    ,(short)173058296	,null	,null	,41816681	,false	,"22f54e5c15c5dd555c3242fdd0cedf8c9f517201bb71986b852c4cb76cae46709260329b665828af3a288f7a1c6bcabd5ec0c101586d18ac64655205a0cb1c68"	,"3dc0bd8706d3a7644d83d8fea7189bec3e5af9a7b78698bd18a20c80f71b8cc1"	,true	        ,null	        ,null	        ,false	                ,false	                 ,0	        ,0	        ,542660	    ,false, null, null);
    public final DGSPurchase PURCHASE_11 = createPurchase(61	,-1155069143692520623L	,3705364957971254799L,-2208439159357779035L	    ,9211698109297098287L,1	,100000000	    ,(short)173058317	,null	,null	,41816705	,false	,"a83ed3a4798ac7629345f0204383f8187209a0db6167048b5634178d5c1e450e90d363068ddc65d93e3b3c4050e52c40803503388dce82d2e735f1a3fcb2086a"	,"be123e5322ab64bbdf047c89c1010bd11d0ec262207c033325e8dda3a185f6b8"	,true	        ,null	        ,null	        ,false	                ,false	                 ,0	        ,1000000	    ,542667	    ,false, null, null);
    public final DGSPurchase PURCHASE_12 = createPurchase(62	,-3910276708092716563L	,3705364957971254799L,-1443882373390424300L	    ,9211698109297098287L,1	,100000000	    ,(short)173058296	,null	,null	,41816681	,false	,"22f54e5c15c5dd555c3242fdd0cedf8c9f517201bb71986b852c4cb76cae46709260329b665828af3a288f7a1c6bcabd5ec0c101586d18ac64655205a0cb1c68"	,"3dc0bd8706d3a7644d83d8fea7189bec3e5af9a7b78698bd18a20c80f71b8cc1"	,true	        ,null	        ,null	        ,false	                ,false	                 ,0	        ,5000000	    ,542672	    ,false, null, null);
    public final DGSPurchase PURCHASE_13 = createPurchase(63	,-3910276708092716563L	,3705364957971254799L,-1443882373390424300L	    ,9211698109297098287L,1	,100000000	    ,(short)173058296	,null	,null	,41816681	,false	,"22f54e5c15c5dd555c3242fdd0cedf8c9f517201bb71986b852c4cb76cae46709260329b665828af3a288f7a1c6bcabd5ec0c101586d18ac64655205a0cb1c68"	,"3dc0bd8706d3a7644d83d8fea7189bec3e5af9a7b78698bd18a20c80f71b8cc1"	,true	        ,null	        ,null	        ,false	                ,true	                 ,0	        ,5000000	    ,542677	    ,false, PURCHASE_6_PUBLIC_FEEDBACKS, null);
    public final DGSPurchase PURCHASE_14 = createPurchase(64	,-3910276708092716563L	,3705364957971254799L,-1443882373390424300L	    ,9211698109297098287L,1	,100000000	    ,(short)173058296	,null	,null	,41816681	,false	,"22f54e5c15c5dd555c3242fdd0cedf8c9f517201bb71986b852c4cb76cae46709260329b665828af3a288f7a1c6bcabd5ec0c101586d18ac64655205a0cb1c68"	,"3dc0bd8706d3a7644d83d8fea7189bec3e5af9a7b78698bd18a20c80f71b8cc1"	,true	        ,null	        ,null	        ,true	                ,true	                 ,0	        ,5000000	    ,542682	    ,true , PURCHASE_6_PUBLIC_FEEDBACKS, PURCHASE_6_FEEDBACKS);
    public final DGSPurchase PURCHASE_15 = createPurchase(65	,-1155069143692520623L	,3705364957971254799L,-2208439159357779035L	    ,9211698109297098287L,1	,100000000	    ,(short)173058317	,null	,null	,41816705	,false	,"a83ed3a4798ac7629345f0204383f8187209a0db6167048b5634178d5c1e450e90d363068ddc65d93e3b3c4050e52c40803503388dce82d2e735f1a3fcb2086a"	,"be123e5322ab64bbdf047c89c1010bd11d0ec262207c033325e8dda3a185f6b8"	,true	        ,null	        ,null	        ,true	                ,false	                 ,0	        ,1000000	    ,542688	    ,false, null, PURCHASE_7_FEEDBACKS);
    public final DGSPurchase PURCHASE_16 = createPurchase(66	,-1155069143692520623L	,3705364957971254799L,-2208439159357779035L	    ,9211698109297098287L,1	,100000000	    ,(short)173058317	,null	,null	,41816705	,false	,"a83ed3a4798ac7629345f0204383f8187209a0db6167048b5634178d5c1e450e90d363068ddc65d93e3b3c4050e52c40803503388dce82d2e735f1a3fcb2086a"	,"be123e5322ab64bbdf047c89c1010bd11d0ec262207c033325e8dda3a185f6b8"	,true	        ,null	        ,null	        ,true	                ,true	                 ,0	        ,1000000	    ,542693	    ,true , PURCHASE_7_PUBLIC_FEEDBACKS, PURCHASE_7_FEEDBACKS);

    
    public final DGSGoods GOODS_0  = createGoods(54,	350597963087434976L	    ,9211698109297098287L	,"Some product"	    ,"Some product Some product"	    ,new String[]{"product","some","ptd"}	,true	,"product some ptd test"    	,41814871	,2	        ,100000000000L	,false	    ,541839	    ,false);
    public final DGSGoods GOODS_1  = createGoods(55,	350597963087434976L	    ,9211698109297098287L	,"Some product"	    ,"Some product Some product"	    ,new String[]{"product","some","ptd"}	,true	,"product some ptd test"    	,41814871	,1	        ,100000000000L	,false	    ,541867	    ,false);
    public final DGSGoods GOODS_2  = createGoods(56,	350597963087434976L	    ,9211698109297098287L	,"Some product"	    ,"Some product Some product"	    ,new String[]{"product","some","ptd"}	,true	,"product some ptd test"    	,41814871	,0	        ,100000000000L	,false	    ,541874	    ,true );
    public final DGSGoods GOODS_3  = createGoods(57,	-1443882373390424300L	,9211698109297098287L	,"Test product"	    ,"Test"	                        ,new String[]{"tag"}	                ,true	,"tag"	                        ,41816306	,1	        ,100000000	    ,false	    ,541986	    ,false);
    public final DGSGoods GOODS_4  = createGoods(58,	-1443882373390424300L	,9211698109297098287L	,"Test product"	    ,"Test"	                        ,new String[]{"tag"}	                ,true	,"tag"	                        ,41816306	,0	        ,100000000	    ,false	    ,542026	    ,true );
    public final DGSGoods GOODS_5  = createGoods(59,	-2208439159357779035L	,9211698109297098287L	,"1"		            , null	                            ,new String[]{}                         ,true	,""	                        ,38796761	,0	        ,100000000	    ,false	    ,542029	    ,true );
    public final DGSGoods GOODS_6  = createGoods(60,	-9001112213900824483L	,3705364957971254799L	,"asdffasdf"	        ,"asdf"	                        ,new String[]{"asdf"}	                ,true	,"asdf"	                    ,37965044	,222	        ,400000000	    ,false	    ,542710	    ,false);
    public final DGSGoods GOODS_7  = createGoods(61,	-9001112213900824483L	,3705364957971254799L	,"asdffasdf"	        ,"asdf"	                        ,new String[]{"asdf"}                   ,true	,"asdf"	                    ,37965044	,2	        ,400000000	    ,false	    ,542712	    ,false);
    public final DGSGoods GOODS_8  = createGoods(62,	-8243930277887618399L	,9211698109297098287L	,"test"	            ,"TEST"	                        ,new String[]{"sport"}	                ,true	,"sport"	                    ,38188829	,21	        ,44400000000L	,true	    ,542717	    ,true );
    public final DGSGoods GOODS_9  = createGoods(63,	-2394458469048114141L	,9211698109297098287L	,"fsf"	            ,"fsdfsd"	                        ,new String[]{}	                        ,true	,"ff"	                        ,38176323	,1	        ,100000000000L	,true	    ,542719	    ,true );
    public final DGSGoods GOODS_10 = createGoods(64,	8788482956389726350L	    ,9211698109297098287L	,"test"	            ,"test"	                        ,new String[]{"test"}	                ,true	,"test"	                    ,38189627	,2	        ,100000000	    ,false	    ,542721	    ,true );
    public final DGSGoods GOODS_11 = createGoods(65,	4948091426407579194L	    ,9211698109297098287L	,"qwe"	            ,"qwe"	                            ,new String[]{"qwe"}	                ,true	,"qwe"	                        ,38039976	,12	        ,100000000  	,false	    ,542725	    ,true );
    public final DGSGoods GOODS_12 = createGoods(66,	2267824434322963112L	    ,9211698109297098287L	,"Another product"	,"Just another produc"	            ,new String[]{"tag","batman"}	        ,true	,"tag batman"	                ,41824604	,3	        ,150000000000L	,false	    ,542828	    ,true );
    public final DGSGoods GOODS_13 = createGoods(67,	-9001112213900824483L	,3705364957971254799L    ,"asdffasdf"	        ,"asdf"	                        ,new String[]{"asdf"}	                ,true	,"asdf"	                    ,37965044	,2	        ,500000000	    ,false	    ,542860	    ,false );

    
    
    
    public final DGSTag TAG_0  = createTag(36	        ,"product"	,2	                ,4	            ,541839	,false    );
    public final DGSTag TAG_1  = createTag(37	        ,"some"	    ,1	                ,1	            ,541839	,false    );
    public final DGSTag TAG_2  = createTag(38	        ,"ptd"	    ,1	                ,1	            ,541839	,false    );
    public final DGSTag TAG_3  = createTag(39	        ,"deleted"	,2	                ,2	            ,541842	,false     );
    public final DGSTag TAG_4  = createTag(40	        ,"product"	,1	                ,4	            ,541874	,true      );
    public final DGSTag TAG_5  = createTag(41	        ,"some"	    ,0	                ,1	            ,541874	,true      );
    public final DGSTag TAG_6  = createTag(42	        ,"ptd"	    ,0	                ,1	            ,541874	,true     );
    public final DGSTag TAG_7  = createTag(43	        ,"tag"	    ,1	                ,1	            ,541986	,false    );
    public final DGSTag TAG_8  = createTag(44	        ,"deleted"	,2	                ,2	            ,541988	,false     );
    public final DGSTag TAG_9  = createTag(45	        ,"tag"	    ,0	                ,1	            ,542026	,false     );
    public final DGSTag TAG_10 = createTag(46	        ,"sport"    ,4	                ,5	            ,542717	,true      );
    public final DGSTag TAG_11 = createTag(47	        ,"tag"	    ,1	                ,2	            ,542828	,true      );
    public final DGSTag TAG_12 = createTag(48	        ,"batman"   ,1	                ,1	            ,542828	,true      );

    



    private DGSPurchase createPurchase(long dbId, long id, long buyerId, long goodsId, long sellerId, int quantity, long price, short deadline, String note, String nonce, int timestamp, boolean pending, String goods, String goodsNonce, boolean goodsIsText, String refundNote, String refundNonce, boolean hasFeedbackNotes, boolean hasPublicFeedbacks, long discount, long refund, int height, boolean latest, List<DGSPublicFeedback> publicFeedbacks, List<DGSFeedback> feedbacks) {
        DGSPurchase dgsPurchase = new DGSPurchase(dbId, height, id, buyerId, goodsId, sellerId, quantity, price, deadline, new EncryptedData(Convert.parseHexString(note), Convert.parseHexString(nonce)), timestamp, pending, new EncryptedData(Convert.parseHexString(goods), Convert.parseHexString(goodsNonce)), goodsIsText, new EncryptedData(Convert.parseHexString(refundNote), Convert.parseHexString(refundNonce)), hasPublicFeedbacks, hasFeedbackNotes, feedbacks, publicFeedbacks, discount, refund);
        dgsPurchase.setLatest(latest);
        return dgsPurchase;
    }
    private DGSFeedback createFeedback(long dbId, long id, String feedbackData, String feedbackNonce, int height, boolean latest) {
        DGSFeedback feedback = new DGSFeedback(dbId, height, id, new EncryptedData(Convert.parseHexString(feedbackData), Convert.parseHexString(feedbackNonce)));
        feedback.setLatest(latest);
        return feedback;
    }
    private DGSPublicFeedback createFeedback(long dbId, long id, String publicFeedback, int height, boolean latest) {
        DGSPublicFeedback dgsPublicFeedback = new DGSPublicFeedback(dbId, height, publicFeedback, id);
        dgsPublicFeedback.setLatest(latest);
        return dgsPublicFeedback;
    }

    private DGSGoods createGoods(long dbId, long id, long sellerId, String name, String description, String[] parsedTags, boolean hasImage, String tags, int timestamp, int quantity, long price, boolean delisted, int height, boolean latest) {
        DGSGoods dgsGoods = new DGSGoods(dbId, height, id, sellerId, name, description, tags, parsedTags, timestamp, hasImage, quantity, price, delisted);
        dgsGoods.setLatest(latest);
        return dgsGoods;
    }

    private DGSTag createTag(long dbId, String tag, int inStockCount, int totalCount, int height, boolean latest) {
        DGSTag dgsTag = new DGSTag(dbId, height, tag, inStockCount, totalCount);
        dgsTag.setLatest(latest);
        return dgsTag;
    }
}
