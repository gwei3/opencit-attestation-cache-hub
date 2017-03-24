/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 * Author:  hmgowda
 * Created: Mar 16, 2017
 */


ALTER TABLE AH_HOST DROP COLUMN AIK_SHA1;
ALTER TABLE AH_HOST ADD COLUMN AIK_SHA256 varchar(100) DEFAULT NULL;
INSERT INTO changelog (ID, APPLIED_AT, DESCRIPTION) VALUES (20170316203000,NOW(),'Removed AIK_SHA1 column and inserted AIK_SHA256');