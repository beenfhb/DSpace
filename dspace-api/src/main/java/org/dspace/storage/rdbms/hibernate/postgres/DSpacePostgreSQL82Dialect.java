/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.rdbms.hibernate.postgres;

import java.sql.Types;

import org.hibernate.dialect.PostgreSQL82Dialect;
import org.hibernate.type.descriptor.sql.LongVarcharTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

/**
 * UUID's are not supported by default in hibernate due to differences in the database in order to fix this a custom sql dialect is needed.
 * Source: https://forum.hibernate.org/viewtopic.php?f=1&t=1014157
 *
 * NOTE: to DOWNGRADE hibernate at version 4.1.8.Final we use another mode to register UUID type
 *
 * @author kevinvandevelde at atmire.com
 */
public class DSpacePostgreSQL82Dialect extends PostgreSQL82Dialect
{
//	--USING with 4.2.21.Final	
//	@Override
//	public void contributeTypes(final TypeContributions typeContributions, final ServiceRegistry serviceRegistry) {
//		super.contributeTypes(typeContributions, serviceRegistry);
//		typeContributions.contributeType(new InternalPostgresUUIDType());
//	}

//	--USING with 4.1.8.Final		
	public DSpacePostgreSQL82Dialect() {
		registerHibernateType(Types.OTHER, "pg-uuid");
	}

    @Override
    protected void registerHibernateType(int code, String name) {
        super.registerHibernateType(code, name);
    }

//	--USING with 4.2.21.Final	
//    protected static class InternalPostgresUUIDType extends PostgresUUIDType {
//
//        @Override
//        protected boolean registerUnderJavaType() {
//            return true;
//        }
//    }

    /**
     * Override is needed to properly support the CLOB on metadatavalue in postgres & oracle.
     * @param sqlCode {@linkplain java.sql.Types JDBC type-code} for the column mapped by this type.
     * @return Descriptor for the SQL/JDBC side of a value mapping.
     */
    @Override
    public SqlTypeDescriptor getSqlTypeDescriptorOverride(int sqlCode) {
        SqlTypeDescriptor descriptor;
        switch (sqlCode) {
            case Types.CLOB: {
                descriptor = LongVarcharTypeDescriptor.INSTANCE;
                break;
            }
            default: {
                descriptor = super.getSqlTypeDescriptorOverride(sqlCode);
                break;
            }
        }
        return descriptor;
    }
}
